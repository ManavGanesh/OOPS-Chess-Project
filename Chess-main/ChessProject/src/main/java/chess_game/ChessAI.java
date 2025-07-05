package chess_game;

import chess_game.Boards.Board;
import chess_game.Move.Move;
import chess_game.Pieces.Team;
import chess_game.Utilities.MoveUtilities;
import chess_game.Player.Player;
import chess_game.Pieces.Piece;
import chess_game.Pieces.PieceTypes;

import java.util.List;
import java.util.concurrent.*;

public class ChessAI {

    private final int MAX_DEPTH;
    private final ExecutorService executor;
    
    // Optimized caching system
    private final java.util.Map<String, Double> evaluationCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, Move> bestMoveCache = new java.util.concurrent.ConcurrentHashMap<>();
    private int cacheHits = 0;
    private int cacheMisses = 0;
    private long lastCacheCleanup = System.currentTimeMillis();
    private static final long CACHE_CLEANUP_INTERVAL = 60000; // 1 minute
    private static final int MAX_CACHE_SIZE = 10000; // Reduced cache size

    public ChessAI(int depth) {
        this.MAX_DEPTH = depth;
        // Use a smaller thread pool for better performance
        this.executor = Executors.newFixedThreadPool(
            Math.min(4, Runtime.getRuntime().availableProcessors())
        );
    }

    public Move getBestMove(Board board, Team aiTeam) {
        // Check cache for this position first
        String boardHash = getSimpleBoardHash(board);
        Move cachedMove = bestMoveCache.get(boardHash + aiTeam.toString());
        if (cachedMove != null) {
            cacheHits++;
            return cachedMove;
        }
        cacheMisses++;
        
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, aiTeam);
        if (legalMoves.isEmpty()) return null;
        
        // 1. CRITICAL: Immediate checkmate detection (1 move)
        Move checkmateMove = findImmediateCheckmate(board, aiTeam);
        if (checkmateMove != null) {
            cacheMove(boardHash, aiTeam, checkmateMove);
            return checkmateMove;
        }
        
        // 2. SAFE QUEEN CAPTURES: Only capture queen if it doesn't create hanging pieces
        Move queenCapture = findOpponentQueenCapture(board, aiTeam);
        if (queenCapture != null && !createHangingPiece(queenCapture, board, aiTeam)) {
            cacheMove(boardHash, aiTeam, queenCapture);
            return queenCapture;
        }
        
        // 3. HIGH PRIORITY: Free captures
        Move freeCapture = findBestFreeCapture(board, aiTeam);
        if (freeCapture != null) {
            cacheMove(boardHash, aiTeam, freeCapture);
            return freeCapture;
        }
        
        // 4. RESCUE: Save hanging pieces (Queen first)
        Move rescue = findCriticalQueenRescue(board, aiTeam);
        if (rescue == null) {
            rescue = findValueBasedRescue(board, aiTeam);
        }
        if (rescue != null) {
            cacheMove(boardHash, aiTeam, rescue);
            return rescue;
        }
        
        // 5. Minimax evaluation for remaining moves
        orderMoves(legalMoves, board, aiTeam);
        
        Move bestMove = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        // Use limited parallelization for performance
        int maxMovesToEvaluate = Math.min(legalMoves.size(), 12);
        for (int i = 0; i < maxMovesToEvaluate; i++) {
            Move move = legalMoves.get(i);
            if (shouldPruneMove(move, board, aiTeam)) continue;
            
            Board newBoard = board.deepCopy();
            Player currentPlayer = newBoard.getCurrentPlayer();
            currentPlayer.makeMove(newBoard, move);
            
            double score = minimax(newBoard, MAX_DEPTH - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, false, aiTeam);
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        if (bestMove == null) bestMove = legalMoves.get(0); // Fallback
        
        // Cache the result
        cacheMove(boardHash, aiTeam, bestMove);
        
        // Periodic cache cleanup
        performPeriodicCacheCleanup();
        
        return bestMove;
    }

    private double minimax(Board board, int depth, double alpha, double beta, boolean maximizingPlayer, Team aiTeam) {
        if (depth == 0 || isGameOver(board, aiTeam)) {
            return evaluateBoard(board, aiTeam);
        }

        Team currentTeam = maximizingPlayer ? aiTeam : getOpponent(aiTeam);
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, currentTeam);

        if (maximizingPlayer) {
            double maxEval = Double.NEGATIVE_INFINITY;
            for (Move move : legalMoves) {
                if (pruneEarly(move, board, currentTeam)) continue;
                Board newBoard = board.deepCopy();
                Player currentPlayer = newBoard.getCurrentPlayer();
                currentPlayer.makeMove(newBoard, move);
                double eval = minimax(newBoard, depth - 1, alpha, beta, false, aiTeam);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            double minEval = Double.POSITIVE_INFINITY;
            for (Move move : legalMoves) {
                if (pruneEarly(move, board, currentTeam)) continue;
                Board newBoard = board.deepCopy();
                Player currentPlayer = newBoard.getCurrentPlayer();
                currentPlayer.makeMove(newBoard, move);
                double eval = minimax(newBoard, depth - 1, alpha, beta, true, aiTeam);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private double evaluateBoard(Board board, Team aiTeam) {
        // Quick cache lookup
        String boardHash = getSimpleBoardHash(board);
        Double cachedScore = evaluationCache.get(boardHash);
        if (cachedScore != null) {
            cacheHits++;
            return cachedScore;
        }
        cacheMisses++;
        
        double score = 0;
        Team opponentTeam = getOpponent(aiTeam);
        
        // 1. MATERIAL BALANCE (80% weight) - Most important
        score += evaluateMaterialBalance(board, aiTeam, opponentTeam) * 0.8;
        
        // 2. TACTICAL THREATS (15% weight) - Critical for tactics
        score += evaluateTacticalThreats(board, aiTeam, opponentTeam) * 0.15;
        
        // 3. POSITION (5% weight) - Basic positional awareness
        score += evaluateBasicPosition(board, aiTeam) * 0.05;
        
        // Cache if space available
        if (evaluationCache.size() < MAX_CACHE_SIZE) {
            evaluationCache.put(boardHash, score);
        }
        
        return score;
    }
    
    /**
     * Simplified material balance evaluation
     */
    private double evaluateMaterialBalance(Board board, Team aiTeam, Team opponentTeam) {
        double aiMaterial = 0;
        double opponentMaterial = 0;
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null) {
                    int points = piece.getPoints();
                    if (piece.getTeam() == aiTeam) {
                        aiMaterial += points;
                    } else {
                        opponentMaterial += points;
                    }
                }
            }
        }
        
        return aiMaterial - opponentMaterial;
    }
    
    /**
     * Simplified tactical threat evaluation
     */
    private double evaluateTacticalThreats(Board board, Team aiTeam, Team opponentTeam) {
        double threatScore = 0;
        
        // Check for hanging pieces (simplified)
        List<Piece> ourHanging = getHangingPieces(board, aiTeam);
        List<Piece> theirHanging = getHangingPieces(board, opponentTeam);
        
        // Penalty for our hanging pieces
        for (Piece piece : ourHanging) {
            threatScore -= piece.getPoints() * 10;
        }
        
        // Bonus for their hanging pieces
        for (Piece piece : theirHanging) {
            threatScore += piece.getPoints() * 8;
        }
        
        return threatScore;
    }
    
    /**
     * Basic positional evaluation
     */
    private double evaluateBasicPosition(Board board, Team aiTeam) {
        double positionScore = 0;
        
        // Center control bonus
        int[][] centerSquares = {{3,3}, {3,4}, {4,3}, {4,4}};
        for (int[] square : centerSquares) {
            Piece piece = board.getTile(square[0], square[1]).getPiece();
            if (piece != null && piece.getTeam() == aiTeam) {
                positionScore += 5;
            }
        }
        
        // Mobility bonus (simplified)
        int ourMobility = MoveUtilities.getLegalMoves(board, aiTeam).size();
        int theirMobility = MoveUtilities.getLegalMoves(board, getOpponent(aiTeam)).size();
        positionScore += (ourMobility - theirMobility) * 0.1;
        
        return positionScore;
    }
    
    /**
     * POINT-BASED THREAT EVALUATION - Pure material calculations
     */
    private double evaluatePointBasedThreats(Board board, Team team) {
        double threatScore = 0;
        
        // 1. Hanging pieces penalty/bonus
        List<Piece> ourHanging = getHangingPieces(board, team);
        List<Piece> theirHanging = getHangingPieces(board, getOpponent(team));
        
        // Heavy penalty for our hanging pieces
        for (Piece piece : ourHanging) {
            threatScore -= piece.getPoints() * 12; // 12x penalty
        }
        
        // Bonus for their hanging pieces
        for (Piece piece : theirHanging) {
            threatScore += piece.getPoints() * 10; // 10x bonus
        }
        
        // 2. Immediate capture opportunities
        List<Move> ourMoves = MoveUtilities.getLegalMoves(board, team);
        for (Move move : ourMoves) {
            if (move.hasKilledPiece()) {
                int capturedValue = move.getKilledPiece().getPoints();
                int attackerValue = move.getMovedPiece().getPoints();
                
                if (capturedValue > attackerValue) {
                    threatScore += (capturedValue - attackerValue) * 3;
                } else if (capturedValue == attackerValue) {
                    threatScore += 1;
                }
            }
            
            // Promotion bonus
            if (move.isPromotionMove()) {
                PieceTypes promotionType = move.getPromotionPieceType();
                int promotionValue = (promotionType != null) ? promotionType.getPoints() : PieceTypes.QUEEN.getPoints();
                threatScore += (promotionValue - 1) * 8; // Gain from pawn to promoted piece
            }
        }
        
        return threatScore;
    }
    
    /**
     * BASIC MOBILITY - Simple move count without piece bias
     */
    private double evaluateBasicMobility(Board board, Team aiTeam, Team opponentTeam) {
        int ourMobility = MoveUtilities.getLegalMoves(board, aiTeam).size();
        int theirMobility = MoveUtilities.getLegalMoves(board, opponentTeam).size();
        
        return (ourMobility - theirMobility) * 0.1; // Small weight for mobility
    }
    
    /**
     * Generates a simple hash for board caching
     */
    private String getBoardHash(Board board) {
        StringBuilder hash = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null) {
                    hash.append(piece.getClass().getSimpleName().charAt(0))
                        .append(piece.getTeam() == Team.WHITE ? "W" : "B")
                        .append(i).append(j);
                }
            }
        }
        return hash.toString();
    }
    
    private double evaluateMaterial(Board board, Team team) {
        double score = 0;
        Team opponent = getOpponent(team);
        
        // Piece-square tables for better positioning
        int[][] pawnTable = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {5, 10, 10, -20, -20, 10, 10, 5},
            {5, -5, -10, 0, 0, -10, -5, 5},
            {0, 0, 0, 20, 20, 0, 0, 0},
            {5, 5, 10, 25, 25, 10, 5, 5},
            {10, 10, 20, 30, 30, 20, 10, 10},
            {50, 50, 50, 50, 50, 50, 50, 50},
            {0, 0, 0, 0, 0, 0, 0, 0}
        };
        
        int[][] knightTable = {
            {-50, -40, -30, -30, -30, -30, -40, -50},
            {-40, -20, 0, 0, 0, 0, -20, -40},
            {-30, 0, 10, 15, 15, 10, 0, -30},
            {-30, 5, 15, 20, 20, 15, 5, -30},
            {-30, 0, 15, 20, 20, 15, 0, -30},
            {-30, 5, 10, 15, 15, 10, 5, -30},
            {-40, -20, 0, 5, 5, 0, -20, -40},
            {-50, -40, -30, -30, -30, -30, -40, -50}
        };
        
        int[][] bishopTable = {
            {-20, -10, -10, -10, -10, -10, -10, -20},
            {-10, 0, 0, 0, 0, 0, 0, -10},
            {-10, 0, 5, 10, 10, 5, 0, -10},
            {-10, 5, 5, 10, 10, 5, 5, -10},
            {-10, 0, 10, 10, 10, 10, 0, -10},
            {-10, 10, 10, 10, 10, 10, 10, -10},
            {-10, 5, 0, 0, 0, 0, 5, -10},
            {-20, -10, -10, -10, -10, -10, -10, -20}
        };
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null) {
                    double baseValue = piece.getPoints();
                    double positionBonus = 0;
                    
                    // Apply piece-square table bonuses
                    int row = (piece.getTeam() == Team.WHITE) ? 7 - i : i;
                    String pieceType = piece.getClass().getSimpleName();
                    
                    switch (pieceType) {
                        case "Pawn":
                            positionBonus = pawnTable[row][j] * 0.01;
                            break;
                        case "Knight":
                            positionBonus = knightTable[row][j] * 0.01;
                            break;
                        case "Bishop":
                            positionBonus = bishopTable[row][j] * 0.01;
                            break;
                    }
                    
                    double totalValue = baseValue + positionBonus;
                    score += (piece.getTeam() == team ? 1 : -1) * totalValue;
                }
            }
        }
        
        return score;
    }
    
    /**
     * Enhanced king safety evaluation including castling importance
     */
    private double evaluateKingSafetyAndCastling(Board board, Team team) {
        // Find king position
        int kingRow = -1, kingCol = -1;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null && piece.getClass().getSimpleName().equals("King") && piece.getTeam() == team) {
                    kingRow = i;
                    kingCol = j;
                    break;
                }
            }
        }
        
        if (kingRow == -1) return -1000; // King not found (shouldn't happen)
        
        double safety = 0;
        
        // CASTLING EVALUATION - Major strategic importance
        safety += evaluateCastlingStatus(board, team, kingRow, kingCol);
        
        // Penalize exposed king
        if (team == Team.WHITE && kingRow > 5) safety -= 50; // King too far forward
        if (team == Team.BLACK && kingRow < 2) safety -= 50;
        
        // Check for pawn shield
        int pawnShield = 0;
        int direction = (team == Team.WHITE) ? -1 : 1;
        
        for (int j = Math.max(0, kingCol - 1); j <= Math.min(7, kingCol + 1); j++) {
            int checkRow = kingRow + direction;
            if (checkRow >= 0 && checkRow < 8) {
                Piece piece = board.getTile(checkRow, j).getPiece();
                if (piece != null && piece.getClass().getSimpleName().equals("Pawn") && piece.getTeam() == team) {
                    pawnShield++;
                }
            }
        }
        
        safety += pawnShield * 15; // Increased importance of pawn shield
        
        // Penalize king in center during opening/middlegame
        if (kingCol >= 2 && kingCol <= 5) safety -= 30; // Increased penalty
        
        return safety;
    }
    
    /**
     * Evaluates castling status - crucial strategic element
     */
    private double evaluateCastlingStatus(Board board, Team team, int kingRow, int kingCol) {
        double castlingScore = 0;
        
        // Check if king has moved (castling no longer possible)
        boolean kingInStartPosition = (team == Team.WHITE && kingRow == 7 && kingCol == 4) ||
                                     (team == Team.BLACK && kingRow == 0 && kingCol == 4);
        
        if (!kingInStartPosition) {
            // Check if king has castled (good)
            if ((team == Team.WHITE && kingRow == 7 && (kingCol == 2 || kingCol == 6)) ||
                (team == Team.BLACK && kingRow == 0 && (kingCol == 2 || kingCol == 6))) {
                castlingScore += 50; // Bonus for successful castling
                
                // Extra bonus for kingside castling (usually safer)
                if (kingCol == 6) {
                    castlingScore += 10;
                }
            } else {
                // King moved without castling - penalty
                castlingScore -= 25;
            }
        } else {
            // King still in starting position - check if castling is still possible
            boolean canCastleKingside = canCastle(board, team, true);
            boolean canCastleQueenside = canCastle(board, team, false);
            
            if (canCastleKingside) castlingScore += 15;
            if (canCastleQueenside) castlingScore += 10;
            
            // Penalty if castling rights are lost due to rook movement
            if (!canCastleKingside && !canCastleQueenside) {
                castlingScore -= 20;
            }
        }
        
        return castlingScore;
    }
    
    /**
     * Simple check for castling possibility (this might need to be enhanced based on your Move class)
     */
    private boolean canCastle(Board board, Team team, boolean kingside) {
        int row = (team == Team.WHITE) ? 7 : 0;
        int rookCol = kingside ? 7 : 0;
        
        // Check if rook is in starting position
        Piece rook = board.getTile(row, rookCol).getPiece();
        if (rook == null || !rook.getClass().getSimpleName().equals("Rook") || rook.getTeam() != team) {
            return false;
        }
        
        // Check if squares between king and rook are empty
        int start = kingside ? 5 : 1;
        int end = kingside ? 6 : 3;
        
        for (int col = start; col <= end; col++) {
            if (board.getTile(row, col).getPiece() != null) {
                return false;
            }
        }
        
        return true; // Simplified - in practice you'd also check for check/threats
    }
    
    /**
     * Evaluates piece coordination - rook-rook, knight-knight connections, etc.
     * This adds strategic depth to the AI's understanding
     */
    private double evaluatePieceCoordination(Board board, Team team) {
        double coordination = 0;
        
        // Find all pieces of each type for this team
        java.util.List<int[]> rooks = new java.util.ArrayList<>();
        java.util.List<int[]> knights = new java.util.ArrayList<>();
        java.util.List<int[]> bishops = new java.util.ArrayList<>();
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null && piece.getTeam() == team) {
                    String pieceType = piece.getClass().getSimpleName();
                    switch (pieceType) {
                        case "Rook":
                            rooks.add(new int[]{i, j});
                            break;
                        case "Knight":
                            knights.add(new int[]{i, j});
                            break;
                        case "Bishop":
                            bishops.add(new int[]{i, j});
                            break;
                    }
                }
            }
        }
        
        // ROOK-ROOK COORDINATION
        if (rooks.size() >= 2) {
            for (int i = 0; i < rooks.size(); i++) {
                for (int j = i + 1; j < rooks.size(); j++) {
                    int[] rook1 = rooks.get(i);
                    int[] rook2 = rooks.get(j);
                    
                    // Connected rooks (same rank or file)
                    if (rook1[0] == rook2[0] || rook1[1] == rook2[1]) {
                        // Check if the connection is clear
                        if (isPathClear(board, rook1[0], rook1[1], rook2[0], rook2[1])) {
                            coordination += 25; // Strong bonus for connected rooks
                            
                            // Extra bonus for rooks on 7th rank (or 2nd for black)
                            int targetRank = (team == Team.WHITE) ? 1 : 6;
                            if (rook1[0] == targetRank && rook2[0] == targetRank) {
                                coordination += 15; // Double rooks on 7th rank
                            }
                        }
                    }
                    
                    // Rooks supporting each other's files
                    if (Math.abs(rook1[1] - rook2[1]) == 1) {
                        coordination += 10; // Adjacent file control
                    }
                }
            }
        }
        
        // KNIGHT-KNIGHT COORDINATION
        if (knights.size() >= 2) {
            for (int i = 0; i < knights.size(); i++) {
                for (int j = i + 1; j < knights.size(); j++) {
                    int[] knight1 = knights.get(i);
                    int[] knight2 = knights.get(j);
                    
                    // Knights supporting each other
                    int distance = Math.abs(knight1[0] - knight2[0]) + Math.abs(knight1[1] - knight2[1]);
                    
                    if (distance <= 4) { // Knights within supportive distance
                        coordination += 15;
                        
                        // Extra bonus for knights controlling center together
                        if (isNearCenter(knight1[0], knight1[1]) && isNearCenter(knight2[0], knight2[1])) {
                            coordination += 10;
                        }
                    }
                    
                    // Knight outposts (knights protecting each other)
                    if (canKnightAttack(knight1[0], knight1[1], knight2[0], knight2[1]) ||
                        canKnightAttack(knight2[0], knight2[1], knight1[0], knight1[1])) {
                        coordination += 12; // Knights defending each other
                    }
                }
            }
        }
        
        // BISHOP PAIR COORDINATION
        if (bishops.size() >= 2) {
            // Check for bishop pair (one on light, one on dark squares)
            boolean hasLightBishop = false;
            boolean hasDarkBishop = false;
            
            for (int[] bishop : bishops) {
                if ((bishop[0] + bishop[1]) % 2 == 0) {
                    hasLightBishop = true;
                } else {
                    hasDarkBishop = true;
                }
            }
            
            if (hasLightBishop && hasDarkBishop) {
                coordination += 30; // Bishop pair bonus
                
                // Extra bonus if both bishops are active
                int activeBishops = 0;
                for (int[] bishop : bishops) {
                    if (isNearCenter(bishop[0], bishop[1]) || 
                        hasLongDiagonal(board, bishop[0], bishop[1], team)) {
                        activeBishops++;
                    }
                }
                
                if (activeBishops >= 2) {
                    coordination += 15; // Active bishop pair
                }
            }
        }
        
        // QUEEN-PIECE COORDINATION
        int[] queenPos = findPiece(board, team, "Queen");
        if (queenPos != null) {
            // Queen-rook battery
            for (int[] rook : rooks) {
                if (queenPos[0] == rook[0] || queenPos[1] == rook[1]) {
                    if (isPathClear(board, queenPos[0], queenPos[1], rook[0], rook[1])) {
                        coordination += 20; // Queen-rook battery
                    }
                }
            }
            
            // Queen-bishop diagonal battery
            for (int[] bishop : bishops) {
                if (Math.abs(queenPos[0] - bishop[0]) == Math.abs(queenPos[1] - bishop[1])) {
                    if (isPathClear(board, queenPos[0], queenPos[1], bishop[0], bishop[1])) {
                        coordination += 18; // Queen-bishop battery
                    }
                }
            }
        }
        
        return coordination;
    }
    
    /**
     * Helper methods for piece coordination
     */
    private boolean isPathClear(Board board, int row1, int col1, int row2, int col2) {
        int rowDir = Integer.compare(row2, row1);
        int colDir = Integer.compare(col2, col1);
        
        int currentRow = row1 + rowDir;
        int currentCol = col1 + colDir;
        
        while (currentRow != row2 || currentCol != col2) {
            if (board.getTile(currentRow, currentCol).getPiece() != null) {
                return false;
            }
            currentRow += rowDir;
            currentCol += colDir;
        }
        
        return true;
    }
    
    private boolean isNearCenter(int row, int col) {
        return row >= 2 && row <= 5 && col >= 2 && col <= 5;
    }
    
    private boolean canKnightAttack(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = Math.abs(fromRow - toRow);
        int colDiff = Math.abs(fromCol - toCol);
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }
    
    private boolean hasLongDiagonal(Board board, int row, int col, Team team) {
        // Check if bishop is on a long diagonal (a1-h8 or a8-h1)
        return (row + col == 7) || (row - col == 0) || Math.abs(row - col) <= 1;
    }
    
    private int[] findPiece(Board board, Team team, String pieceType) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null && piece.getTeam() == team && 
                    piece.getClass().getSimpleName().equals(pieceType)) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }
    
    private double evaluatePieceActivity(Board board, Team team) {
        double activity = 0;
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null && piece.getTeam() == team) {
                    // Reward pieces that are more active/centralized
                    String pieceType = piece.getClass().getSimpleName();
                    
                    switch (pieceType) {
                        case "Knight":
                            // Knights are better in the center
                            if (i >= 2 && i <= 5 && j >= 2 && j <= 5) activity += 10;
                            break;
                        case "Bishop":
                            // Bishops prefer long diagonals
                            if ((i + j) % 2 == 0) { // Light squares
                                activity += Math.min(i, 7-i) + Math.min(j, 7-j);
                            } else { // Dark squares
                                activity += Math.min(i, 7-i) + Math.min(j, 7-j);
                            }
                            break;
                        case "Rook":
                            // Rooks prefer open files and ranks
                            boolean openFile = true;
                            for (int k = 0; k < 8; k++) {
                                if (k != i && board.getTile(k, j).getPiece() != null && 
                                    board.getTile(k, j).getPiece().getClass().getSimpleName().equals("Pawn")) {
                                    openFile = false;
                                    break;
                                }
                            }
                            if (openFile) activity += 15;
                            break;
                        case "Queen":
                            // Queen prefers central positions but not too early
                            if (i >= 2 && i <= 5 && j >= 2 && j <= 5) activity += 5;
                            break;
                    }
                }
            }
        }
        
        return activity;
    }
    
    private double evaluatePawnStructure(Board board, Team team) {
        double structure = 0;
        boolean[] pawnFiles = new boolean[8];
        
        // Identify pawn positions
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null && piece.getClass().getSimpleName().equals("Pawn") && piece.getTeam() == team) {
                    pawnFiles[j] = true;
                    
                    // Check for doubled pawns
                    for (int k = i + 1; k < 8; k++) {
                        Piece other = board.getTile(k, j).getPiece();
                        if (other != null && other.getClass().getSimpleName().equals("Pawn") && other.getTeam() == team) {
                            structure -= 10; // Penalty for doubled pawns
                        }
                    }
                    
                    // Check for isolated pawns
                    boolean isolated = true;
                    if (j > 0) {
                        for (int k = 0; k < 8; k++) {
                            Piece left = board.getTile(k, j-1).getPiece();
                            if (left != null && left.getClass().getSimpleName().equals("Pawn") && left.getTeam() == team) {
                                isolated = false;
                                break;
                            }
                        }
                    }
                    if (j < 7 && isolated) {
                        for (int k = 0; k < 8; k++) {
                            Piece right = board.getTile(k, j+1).getPiece();
                            if (right != null && right.getClass().getSimpleName().equals("Pawn") && right.getTeam() == team) {
                                isolated = false;
                                break;
                            }
                        }
                    }
                    if (isolated) structure -= 15;
                    
                    // Bonus for passed pawns
                    boolean passed = true;
                    int direction = (team == Team.WHITE) ? -1 : 1;
                    for (int k = i + direction; k >= 0 && k < 8; k += direction) {
                        for (int l = Math.max(0, j-1); l <= Math.min(7, j+1); l++) {
                            Piece blocker = board.getTile(k, l).getPiece();
                            if (blocker != null && blocker.getClass().getSimpleName().equals("Pawn") && blocker.getTeam() != team) {
                                passed = false;
                                break;
                            }
                        }
                        if (!passed) break;
                    }
                    if (passed) {
                        int advancement = (team == Team.WHITE) ? 7 - i : i;
                        structure += advancement * 5;
                    }
                }
            }
        }
        
        return structure;
    }
    
    private double evaluateCenterControl(Board board, Team team) {
        double control = 0;
        int[] centerSquares = {27, 28, 35, 36}; // d4, e4, d5, e5 in linear notation
        
        for (int square : centerSquares) {
            int row = square / 8;
            int col = square % 8;
            
            Piece piece = board.getTile(row, col).getPiece();
            if (piece != null && piece.getTeam() == team) {
                control += 10;
            }
            
            // Check if square is attacked by team
            List<Move> moves = MoveUtilities.getLegalMoves(board, team);
            for (Move move : moves) {
                int destRow = move.getDestinationTile().getCoordinate().getX();
                int destCol = move.getDestinationTile().getCoordinate().getY();
                if (destRow == row && destCol == col) {
                    control += 2;
                    break;
                }
            }
        }
        
        return control;
    }
    
    private double evaluateMobility(Board board, Team team, Team opponent) {
        int teamMobility = MoveUtilities.getLegalMoves(board, team).size();
        int opponentMobility = MoveUtilities.getLegalMoves(board, opponent).size();
        
        double mobilityScore = (teamMobility - opponentMobility) * 0.5;
        
        // Bonus for having more mobility in the opening
        if (teamMobility > opponentMobility * 1.5) {
            mobilityScore += 10;
        }
        
        return mobilityScore;
    }
    
    private double evaluateTacticalThreats(Board board, Team team) {
        double threats = 0;
        
        // 1. MAJOR PENALTY for our own hanging pieces
        threats -= evaluateOwnHangingPieces(board, team);
        
        // 2. Find hanging pieces (opponent's pieces that can be captured for free)
        threats += evaluateHangingPieces(board, team, getOpponent(team));
        
        // 3. Evaluate immediate captures
        threats += evaluateImmediateCaptures(board, team);
        
        // 4. Check for tactical patterns (checks, pins, forks)
        threats += evaluateTacticalPatterns(board, team);
        
        return threats;
    }
    
    /**
     * Evaluates our own hanging pieces - MAJOR penalty to encourage protection
     * Now includes evaluation of defensive options efficiency
     */
    private double evaluateOwnHangingPieces(Board board, Team team) {
        double penalty = 0;
        List<Piece> ourHangingPieces = getHangingPieces(board, team);
        
        for (Piece hangingPiece : ourHangingPieces) {
            // Find the best defensive option for this hanging piece
            DefensiveOption bestDefense = findBestDefensiveOption(board, hangingPiece, team);
            
            if (bestDefense == null) {
                // No defensive option available - MASSIVE penalty
                penalty += hangingPiece.getPoints() * 50; // Increased from 20 to 50
                if (hangingPiece.getPoints() >= 5) {
                    penalty += hangingPiece.getPoints() * 25; // Increased from 8 to 25
                }
                // Special queen penalty
                if (hangingPiece.getPoints() >= 9) {
                    penalty += 200; // Extra 200 point penalty for hanging queen
                }
            } else if (bestDefense.type == DefenseType.RETREAT) {
                // Retreat available - small penalty to encourage it
                penalty += hangingPiece.getPoints() * 2; // Reduced from 3 to 2
            } else if (bestDefense.type == DefenseType.EFFICIENT_DEFENSE) {
                // Can defend with cheaper piece - moderate penalty
                penalty += hangingPiece.getPoints() * 5; // Reduced from 8 to 5
            } else {
                // Only inefficient defense available - high penalty
                penalty += hangingPiece.getPoints() * 30; // Increased from 15 to 30
                if (hangingPiece.getPoints() >= 5) {
                    penalty += hangingPiece.getPoints() * 15; // Increased from 5 to 15
                }
            }
        }
        
        return penalty;
    }
    
    /**
     * Finds the best defensive option for a hanging piece
     */
    private DefensiveOption findBestDefensiveOption(Board board, Piece hangingPiece, Team team) {
        // Find the position of the hanging piece
        int pieceRow = -1, pieceCol = -1;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board.getTile(i, j).getPiece() == hangingPiece) {
                    pieceRow = i;
                    pieceCol = j;
                    break;
                }
            }
        }
        
        if (pieceRow == -1) return null;
        
        List<Move> allMoves = MoveUtilities.getLegalMoves(board, team);
        DefensiveOption bestOption = null;
        
        // Check for retreat options (moving the threatened piece)
        for (Move move : allMoves) {
            if (move.getMovedPiece() == hangingPiece) {
                // This move retreats the hanging piece
                Board testBoard = board.deepCopy();
                Player currentPlayer = testBoard.getCurrentPlayer();
                currentPlayer.makeMove(testBoard, move);
                
                // Check if the piece is still hanging after the move
                if (!isPieceStillHanging(testBoard, move.getDestinationTile().getCoordinate(), team)) {
                    if (bestOption == null || bestOption.type != DefenseType.RETREAT) {
                        bestOption = new DefensiveOption(DefenseType.RETREAT, move, 0);
                    }
                }
            }
        }
        
        // If retreat is available, prefer it
        if (bestOption != null && bestOption.type == DefenseType.RETREAT) {
            return bestOption;
        }
        
        // Check for defensive options (other pieces defending)
        for (Move move : allMoves) {
            if (move.getMovedPiece() != hangingPiece) {
                Board testBoard = board.deepCopy();
                Player currentPlayer = testBoard.getCurrentPlayer();
                currentPlayer.makeMove(testBoard, move);
                
                // Check if this move defends the hanging piece
                if (!isPieceStillHanging(testBoard, new chess_game.Pieces.Coordinate(pieceRow, pieceCol), team)) {
                    int defenderValue = move.getMovedPiece().getPoints();
                    int defendedValue = hangingPiece.getPoints();
                    
                    if (defenderValue < defendedValue) {
                        // Efficient defense
                        if (bestOption == null || bestOption.type == DefenseType.INEFFICIENT_DEFENSE) {
                            bestOption = new DefensiveOption(DefenseType.EFFICIENT_DEFENSE, move, defenderValue);
                        }
                    } else {
                        // Inefficient defense
                        if (bestOption == null) {
                            bestOption = new DefensiveOption(DefenseType.INEFFICIENT_DEFENSE, move, defenderValue);
                        }
                    }
                }
            }
        }
        
        return bestOption;
    }
    
    /**
     * Checks if a piece at the given coordinate is still hanging
     */
    private boolean isPieceStillHanging(Board board, chess_game.Pieces.Coordinate coord, Team team) {
        Team opponent = getOpponent(team);
        List<Move> opponentMoves = MoveUtilities.getLegalMoves(board, opponent);
        
        for (Move opponentMove : opponentMoves) {
            if (opponentMove.hasKilledPiece()) {
                int destRow = opponentMove.getDestinationTile().getCoordinate().getX();
                int destCol = opponentMove.getDestinationTile().getCoordinate().getY();
                
                if (destRow == coord.getX() && destCol == coord.getY()) {
                    // Check if this capture would be truly free
                    if (isFreeCapture(board, opponentMove, opponent)) {
                        return true; // Still hanging
                    }
                }
            }
        }
        
        return false; // Not hanging anymore
    }
    
    /**
     * Enum for different types of defensive options
     */
    private enum DefenseType {
        RETREAT,              // Move the threatened piece to safety
        EFFICIENT_DEFENSE,    // Defend with a less valuable piece
        INEFFICIENT_DEFENSE   // Defend with an equal or more valuable piece
    }
    
    /**
     * Class to represent a defensive option
     */
    private static class DefensiveOption {
        DefenseType type;
        Move move;
        int cost;  // Cost of the defensive move (piece value)
        
        DefensiveOption(DefenseType type, Move move, int cost) {
            this.type = type;
            this.move = move;
            this.cost = cost;
        }
    }
    
    /**
     * Evaluates hanging pieces - pieces that can be captured without retaliation
     * This is the core of the point-based threat detection system
     */
    private double evaluateHangingPieces(Board board, Team team, Team opponent) {
        double score = 0;
        List<Move> ourMoves = MoveUtilities.getLegalMoves(board, team);
        
        for (Move move : ourMoves) {
            if (move.hasKilledPiece()) {
                Piece capturedPiece = move.getKilledPiece();
                Piece attackingPiece = move.getMovedPiece();
                
                // Check if this capture is "free" (no immediate retaliation)
                if (isFreeCapture(board, move, team)) {
                    // High bonus for free captures
                    score += capturedPiece.getPoints() * 10;
                } else {
                    // Check if it's a good trade even with retaliation
                    int materialGain = calculateMaterialGain(board, move, team, 3);
                    if (materialGain > 0) {
                        score += materialGain * 2;
                    }
                }
            }
        }
        
        return score;
    }
    
    /**
     * Checks if a capture is "free" - no immediate retaliation possible
     * Simplified without caching for performance
     */
    private boolean isFreeCapture(Board board, Move captureMove, Team team) {
        
        // Simulate the capture
        Board testBoard = board.deepCopy();
        Player currentPlayer = testBoard.getCurrentPlayer();
        currentPlayer.makeMove(testBoard, captureMove);
        
        // Check if opponent can recapture on the same square
        int destRow = captureMove.getDestinationTile().getCoordinate().getX();
        int destCol = captureMove.getDestinationTile().getCoordinate().getY();
        
        Team opponent = getOpponent(team);
        List<Move> opponentMoves = MoveUtilities.getLegalMoves(testBoard, opponent);
        
        // Optimized check - only look for captures on the target square
        boolean canRecapture = false;
        for (Move opponentMove : opponentMoves) {
            if (opponentMove.hasKilledPiece()) {
                int opponentDestRow = opponentMove.getDestinationTile().getCoordinate().getX();
                int opponentDestCol = opponentMove.getDestinationTile().getCoordinate().getY();
                
                if (opponentDestRow == destRow && opponentDestCol == destCol) {
                    canRecapture = true;
                    break; // Early exit for performance
                }
            }
        }
        
        return !canRecapture;
    }
    
    /**
     * Calculates net material gain/loss for a move considering opponent's best response
     * Uses limited depth search for performance
     */
    private int calculateMaterialGain(Board board, Move move, Team team, int depth) {
        if (depth <= 0) return 0;
        
        Board testBoard = board.deepCopy();
        Player currentPlayer = testBoard.getCurrentPlayer();
        currentPlayer.makeMove(testBoard, move);
        
        int initialGain = move.hasKilledPiece() ? move.getKilledPiece().getPoints() : 0;
        
        // Find opponent's best response
        Team opponent = getOpponent(team);
        List<Move> opponentMoves = MoveUtilities.getLegalMoves(testBoard, opponent);
        
        int bestOpponentGain = 0;
        for (Move opponentMove : opponentMoves) {
            if (opponentMove.hasKilledPiece()) {
                int opponentDestRow = opponentMove.getDestinationTile().getCoordinate().getX();
                int opponentDestCol = opponentMove.getDestinationTile().getCoordinate().getY();
                int moveDestRow = move.getDestinationTile().getCoordinate().getX();
                int moveDestCol = move.getDestinationTile().getCoordinate().getY();
                
                // If opponent can recapture on same square
                if (opponentDestRow == moveDestRow && opponentDestCol == moveDestCol) {
                    int recaptureGain = opponentMove.getKilledPiece().getPoints();
                    // Recursively check our response
                    int futureGain = calculateMaterialGain(testBoard, opponentMove, opponent, depth - 1);
                    bestOpponentGain = Math.max(bestOpponentGain, recaptureGain - futureGain);
                }
            }
        }
        
        return initialGain - bestOpponentGain;
    }
    
    /**
     * Evaluates immediate capture opportunities
     */
    private double evaluateImmediateCaptures(Board board, Team team) {
        double score = 0;
        List<Move> moves = MoveUtilities.getLegalMoves(board, team);
        
        for (Move move : moves) {
            if (move.hasKilledPiece()) {
                int capturedValue = move.getKilledPiece().getPoints();
                int attackerValue = move.getMovedPiece().getPoints();
                
                if (capturedValue > attackerValue) {
                    score += (capturedValue - attackerValue) * 5; // Good trade
                } else if (capturedValue == attackerValue) {
                    score += 2; // Equal trade
                }
            }
        }
        
        return score;
    }
    
    /**
     * Evaluates tactical patterns like checks, pins, forks
     */
    private double evaluateTacticalPatterns(Board board, Team team) {
        double score = 0;
        List<Move> moves = MoveUtilities.getLegalMoves(board, team);
        
        for (Move move : moves) {
            // Bonus for promotion moves
            if (move.isPromotionMove()) {
                PieceTypes promotionType = move.getPromotionPieceType();
                if (promotionType != null) {
                    score += promotionType.getPoints() * 8; // High bonus for promotion
                } else {
                    score += PieceTypes.QUEEN.getPoints() * 8; // Default to queen value
                }
            }
            
            Board testBoard = board.deepCopy();
            Player currentPlayer = testBoard.getCurrentPlayer();
            currentPlayer.makeMove(testBoard, move);
            
            String gameState = MoveUtilities.getGameState(testBoard, getOpponent(team));
            if (gameState != null) {
                if ("CHECKMATE".equals(gameState)) {
                    score += 1000; // Checkmate is best
                } else if ("CHECK".equals(gameState)) {
                    score += 15; // Check is valuable
                }
            }
        }
        
        return score;
    }
    
    private double evaluateEndgame(Board board, Team team, Team opponent) {
        // Count total material to determine if we're in endgame
        int totalMaterial = 0;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null && !piece.getClass().getSimpleName().equals("King")) {
                    totalMaterial += piece.getPoints();
                }
            }
        }
        
        // Endgame considerations (when total material < 20)
        if (totalMaterial < 20) {
            double endgameScore = 0;
            
            // King activity becomes more important
            int kingRow = -1, kingCol = -1;
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    Piece piece = board.getTile(i, j).getPiece();
                    if (piece != null && piece.getClass().getSimpleName().equals("King") && piece.getTeam() == team) {
                        kingRow = i;
                        kingCol = j;
                        break;
                    }
                }
            }
            
            if (kingRow != -1) {
                // King should be centralized in endgame
                double centerDistance = Math.abs(kingRow - 3.5) + Math.abs(kingCol - 3.5);
                endgameScore += (7 - centerDistance) * 2;
            }
            
            return endgameScore;
        }
        
        return 0;
    }

    private boolean pruneEarly(Move move, Board board, Team team) {
        // Enhanced threat detection system
        
        // 1. CRITICAL: Prune moves that create hanging pieces (except for extremely good captures)
        if (createHangingPiece(move, board, team)) {
            int attackerValue = move.getMovedPiece().getPoints();
            
            // If it's a capture that's worth more than the piece being hung, allow it
            if (move.hasKilledPiece()) {
                int capturedValue = move.getKilledPiece().getPoints();
                
                // ENHANCED: Stricter requirements for valuable pieces
                if (attackerValue >= 3) { // Valuable pieces (Bishop, Knight, Rook, Queen)
                    // For queen captures with valuable pieces, need HUGE advantage
                    if (move.getKilledPiece().getClass().getSimpleName().equals("Queen")) {
                        if (capturedValue >= attackerValue + 7) { // Need at least 7 point advantage
                            System.out.println("PRUNING: Allowing valuable piece " + 
                                             move.getMovedPiece().getClass().getSimpleName() +
                                             " to hang for MASSIVE queen capture gain");
                            return false; // Allow extremely favorable queen capture
                        }
                    } else {
                        // For other captures with valuable pieces, need very large advantage
                        if (capturedValue >= attackerValue + 4) { // Need at least 4 point advantage
                            System.out.println("PRUNING: Allowing valuable piece " + 
                                             move.getMovedPiece().getClass().getSimpleName() +
                                             " to hang for very favorable capture");
                            return false;
                        }
                    }
                } else { // Pawns - use original logic
                    // For queen captures, need much larger advantage
                    if (move.getKilledPiece().getClass().getSimpleName().equals("Queen")) {
                        if (capturedValue >= attackerValue + 6) { // Need at least 6 point advantage for queen
                            return false; // Allow very favorable queen capture
                        }
                    } else {
                        // For other pieces, need at least 2 point advantage
                        if (capturedValue > attackerValue + 1) {
                            return false; // Allow the favorable capture even if piece hangs
                        }
                    }
                }
            }
            
            // Otherwise, prune moves that create hanging pieces
            System.out.println("PRUNING: Hanging " + move.getMovedPiece().getClass().getSimpleName() + 
                             " (" + attackerValue + " pts) move - REJECTED");
            return true;
        }
        
        // 2. Check if this move puts our piece in general danger
        if (isPieceInDanger(move, board, team)) {
            // If it's a capture that's worth it, allow it
            if (move.hasKilledPiece()) {
                int capturedValue = move.getKilledPiece().getPoints();
                int attackerValue = move.getMovedPiece().getPoints();
                
                // Only allow if we're capturing something more valuable or equal
                if (capturedValue >= attackerValue) {
                    return false; // Allow the move
                }
            }
            
            // Otherwise, prune moves that put valuable pieces in danger
            return true;
        }
        
        // 3. Original trade evaluation logic
        if (move.hasKilledPiece() &&
            move.getKilledPiece().getPoints() > move.getMovedPiece().getPoints()) {
            return false; // good trade
        }
        
        if (move.getKilledPiece() != null &&
            move.getKilledPiece().getPoints() <= 1) {
            return true; // skip bad trade
        }
        
        return false;
    }

    private boolean isGameOver(Board board, Team team) {
        String state = MoveUtilities.getGameState(board, team);
        return state != null && (state.equals("CHECKMATE") || state.equals("STALEMATE"));
    }

    private Team getOpponent(Team team) {
        return (team == Team.WHITE) ? Team.BLACK : Team.WHITE;
    }

    // PURE POINT-BASED MOVE ORDERING
    private void orderMoves(List<Move> moves, Board board, Team team) {
        moves.sort((a, b) -> {
            double aScore = calculateMoveOrderingScore(a, board, team);
            double bScore = calculateMoveOrderingScore(b, board, team);
            return Double.compare(bScore, aScore); // Higher score first
        });
    }
    
    /**
     * PURE POINT-BASED MOVE SCORING for ordering
     */
    private double calculateMoveOrderingScore(Move move, Board board, Team team) {
        double score = 0;
        
        // 1. CRITICAL: Heavy penalty for moves that create hanging pieces (HIGHEST PRIORITY)
        if (createHangingPiece(move, board, team)) {
            int pieceValue = move.getMovedPiece().getPoints();
            
            // ENHANCED: Extra strict penalties for valuable pieces
            int basePenalty = pieceValue * 200;
            
            // Special handling for bishops, knights, rooks, queens
            if (pieceValue >= 3) {
                basePenalty = pieceValue * 300; // Even higher penalty for valuable pieces
                System.out.println("MOVE ORDERING: ULTRA-SEVERE penalty for hanging valuable " + 
                                 move.getMovedPiece().getClass().getSimpleName() + 
                                 " worth " + pieceValue + " points! Penalty: " + basePenalty);
            }
            
            // Exception: Allow hanging only for extremely favorable queen captures
            if (move.hasKilledPiece() && move.getKilledPiece().getClass().getSimpleName().equals("Queen")) {
                int queenValue = move.getKilledPiece().getPoints();
                int netGain = queenValue - pieceValue;
                
                // For valuable pieces hanging, need even better advantage
                int requiredGain = pieceValue >= 3 ? 7 : 6;
                
                if (netGain >= requiredGain) {
                    score += netGain * 20; // Moderate bonus for very favorable queen trade
                    System.out.println("MOVE ORDERING: Allowing favorable queen trade despite hanging " +
                                     move.getMovedPiece().getClass().getSimpleName() + 
                                     " (net gain: " + netGain + " points)");
                } else {
                    score -= basePenalty;
                    System.out.println("MOVE ORDERING: SEVERE penalty for hanging " + 
                                     move.getMovedPiece().getClass().getSimpleName() + 
                                     " worth " + pieceValue + " points! Net gain (" + netGain + 
                                     ") insufficient (need " + requiredGain + ")!");
                }
            } else {
                score -= basePenalty;
                System.out.println("MOVE ORDERING: SEVERE penalty for hanging " + 
                                 move.getMovedPiece().getClass().getSimpleName() + 
                                 " worth " + pieceValue + " points!");
            }
        }
        // 2. Free captures get very high priority (captured points * 100)
        else if (move.hasKilledPiece() && isFreeCapture(board, move, team)) {
            score += move.getKilledPiece().getPoints() * 100;
        }
        // 3. QUEEN CAPTURES get moderate priority when safe (captured points * 30)
        else if (move.hasKilledPiece() && move.getKilledPiece().getClass().getSimpleName().equals("Queen")) {
            score += move.getKilledPiece().getPoints() * 30; // Reduced from 200x to 30x
            System.out.println("MOVE ORDERING: Safe queen capture gets moderate priority boost");
        }
        
        // 4. DOOMED PIECE LOGIC - If valuable piece is doomed, capture highest value target
        else if (isValuablePiece(move.getMovedPiece()) && isPieceDoomed(board, move.getMovedPiece(), team)) {
            if (move.hasKilledPiece()) {
                // Doomed piece capturing something - prioritize by captured value
                score += move.getKilledPiece().getPoints() * 80; // Very high priority
            } else {
                // Doomed piece moving without capturing - low priority
                score -= move.getMovedPiece().getPoints() * 5;
            }
        }
        // 5. Moving hanging pieces to safety (piece value * 50) - only if not doomed
        else {
            List<Piece> hangingPieces = getHangingPieces(board, team);
            for (Piece hanging : hangingPieces) {
                if (move.getMovedPiece() == hanging) {
                    // Check if move actually saves the piece
                    Board testBoard = board.deepCopy();
                    Player currentPlayer = testBoard.getCurrentPlayer();
                    currentPlayer.makeMove(testBoard, move);
                    
                    if (!isPieceStillHanging(testBoard, move.getDestinationTile().getCoordinate(), team)) {
                        score += hanging.getPoints() * 50;
                    }
                    break;
                }
            }
        }
        
        // 6. Promotion moves (gain in piece value * 30)
        if (move.isPromotionMove()) {
            PieceTypes promotionType = move.getPromotionPieceType();
            int promotionValue = (promotionType != null) ? promotionType.getPoints() : PieceTypes.QUEEN.getPoints();
            score += (promotionValue - 1) * 30; // Subtract pawn value
        }
        
        // 7. Regular captures by net point gain (positive trades * 20)
        if (move.hasKilledPiece() && !move.getKilledPiece().getClass().getSimpleName().equals("Queen")) {
            int netGain = move.getKilledPiece().getPoints() - move.getMovedPiece().getPoints();
            if (netGain > 0) {
                score += netGain * 20;
            } else if (netGain == 0) {
                score += 5; // Equal trades get small bonus
            }
        }
        
        // 8. Small penalty for moving valuable pieces without gain (-piece value * 0.1)
        if (!move.hasKilledPiece() && !move.isPromotionMove()) {
            int pieceValue = move.getMovedPiece().getPoints();
            if (pieceValue >= 5) { // Rook, Queen
                score -= pieceValue * 0.1;
            }
        }
        
        return score;
    }
    
    /**
     * Checks if a move saves one of our hanging pieces
     * Now considers the efficiency of the defensive method
     */
    private boolean savesHangingPiece(Board board, Move move, Team team) {
        // Get current hanging pieces
        List<Piece> hangingPieces = getHangingPieces(board, team);
        
        if (hangingPieces.isEmpty()) return false;
        
        // Check if this move directly moves a hanging piece to safety
        for (Piece hangingPiece : hangingPieces) {
            if (move.getMovedPiece() == hangingPiece) {
                // This is the threatened piece moving to safety - highly preferred!
                return true;
            }
        }
        
        // If it's not moving the threatened piece, check if it's defending it
        // But we'll be more selective about defensive moves
        Board testBoard = board.deepCopy();
        Player currentPlayer = testBoard.getCurrentPlayer();
        currentPlayer.makeMove(testBoard, move);
        
        List<Piece> newHangingPieces = getHangingPieces(testBoard, team);
        
        // Only consider this a "saving" move if it's defending efficiently
        if (newHangingPieces.size() < hangingPieces.size()) {
            // Check if we're defending with a less valuable piece
            Piece defender = move.getMovedPiece();
            for (Piece saved : hangingPieces) {
                if (!newHangingPieces.contains(saved)) {
                    // This piece was saved - check if defense is efficient
                    if (defender.getPoints() >= saved.getPoints()) {
                        return false; // Inefficient defense - don't prioritize
                    }
                }
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Gets all hanging pieces for a team
     */
    private List<Piece> getHangingPieces(Board board, Team team) {
        List<Piece> hangingPieces = new java.util.ArrayList<>();
        Team opponent = getOpponent(team);
        List<Move> opponentMoves = MoveUtilities.getLegalMoves(board, opponent);
        
        for (Move opponentMove : opponentMoves) {
            if (opponentMove.hasKilledPiece()) {
                Piece targetPiece = opponentMove.getKilledPiece();
                
                // Check if this piece is truly hanging (no adequate defense)
                if (targetPiece.getTeam() == team && isPieceHanging(board, opponentMove, team)) {
                    hangingPieces.add(targetPiece);
                }
            }
        }
        
        return hangingPieces;
    }
    
    /**
     * Checks if a piece is truly hanging (can be captured without adequate compensation)
     */
    private boolean isPieceHanging(Board board, Move captureMove, Team defendingTeam) {
        Piece capturedPiece = captureMove.getKilledPiece();
        Piece attackingPiece = captureMove.getMovedPiece();
        
        // Simulate the capture
        Board testBoard = board.deepCopy();
        Player currentPlayer = testBoard.getCurrentPlayer();
        currentPlayer.makeMove(testBoard, captureMove);
        
        // Check if we can recapture
        int captureRow = captureMove.getDestinationTile().getCoordinate().getX();
        int captureCol = captureMove.getDestinationTile().getCoordinate().getY();
        
        List<Move> defenderMoves = MoveUtilities.getLegalMoves(testBoard, defendingTeam);
        
        for (Move defenderMove : defenderMoves) {
            if (defenderMove.hasKilledPiece()) {
                int defenderDestRow = defenderMove.getDestinationTile().getCoordinate().getX();
                int defenderDestCol = defenderMove.getDestinationTile().getCoordinate().getY();
                
                // If we can recapture
                if (defenderDestRow == captureRow && defenderDestCol == captureCol) {
                    Piece recapturedPiece = defenderMove.getKilledPiece();
                    Piece recapturingPiece = defenderMove.getMovedPiece();
                    
                    // Check if the recapture is adequate
                    if (recapturedPiece.getPoints() >= capturedPiece.getPoints()) {
                        return false; // Not hanging - adequate defense
                    }
                }
            }
        }
        
        return true; // Piece is hanging
    }

    // Alias for pruneEarly for compatibility
    private boolean shouldPruneMove(Move move, Board board, Team team) {
        return pruneEarly(move, board, team);
    }

    /**
     * Checks if a piece will be in danger after making a move
     * @param move The move to evaluate
     * @param board Current board state
     * @param team The team making the move
     * @return true if the piece will be in danger, false otherwise
     */
    private boolean isPieceInDanger(Move move, Board board, Team team) {
        // Create a copy of the board to simulate the move
        Board testBoard = board.deepCopy();
        Player currentPlayer = testBoard.getCurrentPlayer();
        
        // Make the move on the test board
        currentPlayer.makeMove(testBoard, move);
        
        // Get the destination position after the move
        int destRow = move.getDestinationTile().getCoordinate().getX();
        int destCol = move.getDestinationTile().getCoordinate().getY();
        
        // Check if the piece at the destination can be captured by the opponent
        Team opponent = getOpponent(team);
        List<Move> opponentMoves = MoveUtilities.getLegalMoves(testBoard, opponent);
        
        for (Move opponentMove : opponentMoves) {
            // Check if opponent can capture our piece
            if (opponentMove.hasKilledPiece()) {
                int captureRow = opponentMove.getDestinationTile().getCoordinate().getX();
                int captureCol = opponentMove.getDestinationTile().getCoordinate().getY();
                
                // If opponent can capture the piece we just moved
                if (captureRow == destRow && captureCol == destCol) {
                    Piece capturedPiece = opponentMove.getKilledPiece();
                    Piece capturingPiece = opponentMove.getMovedPiece();
                    
                    // Check if this is a bad trade (losing more valuable piece)
                    if (capturedPiece.getPoints() > capturingPiece.getPoints()) {
                        return true; // Our piece is in danger of being captured for a loss
                    }
                    
                    // For equal or lesser value pieces, check if we have adequate protection
                    if (!isPieceProtected(testBoard, destRow, destCol, team)) {
                        return true; // Piece is unprotected and can be captured
                    }
                }
            }
        }
        
        return false; // Piece is not in immediate danger
    }
    
    /**
     * Checks if a piece at the given position is protected by friendly pieces
     * @param board The board state
     * @param row Row of the piece
     * @param col Column of the piece
     * @param team The team of the piece
     * @return true if the piece is protected, false otherwise
     */
    private boolean isPieceProtected(Board board, int row, int col, Team team) {
        List<Move> friendlyMoves = MoveUtilities.getLegalMoves(board, team);
        
        for (Move move : friendlyMoves) {
            int destRow = move.getDestinationTile().getCoordinate().getX();
            int destCol = move.getDestinationTile().getCoordinate().getY();
            
            // Check if a friendly piece can move to defend this position
            if (destRow == row && destCol == col) {
                return true; // Position is defended
            }
        }
        
        return false; // Position is not defended
    }
    
    /**
     * ENHANCED: More thorough protection check for valuable pieces
     * Considers the value of defending pieces vs attacking pieces
     */
    private boolean hasAdequateProtection(Board board, int row, int col, Team team, int pieceValue) {
        List<Move> friendlyMoves = MoveUtilities.getLegalMoves(board, team);
        List<Move> opponentMoves = MoveUtilities.getLegalMoves(board, getOpponent(team));
        
        // Count attackers and defenders with their values
        List<Integer> attackerValues = new java.util.ArrayList<>();
        List<Integer> defenderValues = new java.util.ArrayList<>();
        
        // Find all opponent pieces that can attack this square
        for (Move opponentMove : opponentMoves) {
            if (opponentMove.hasKilledPiece()) {
                int destRow = opponentMove.getDestinationTile().getCoordinate().getX();
                int destCol = opponentMove.getDestinationTile().getCoordinate().getY();
                
                if (destRow == row && destCol == col) {
                    attackerValues.add(opponentMove.getMovedPiece().getPoints());
                }
            }
        }
        
        // Find all friendly pieces that can defend this square
        for (Move friendlyMove : friendlyMoves) {
            int destRow = friendlyMove.getDestinationTile().getCoordinate().getX();
            int destCol = friendlyMove.getDestinationTile().getCoordinate().getY();
            
            if (destRow == row && destCol == col) {
                defenderValues.add(friendlyMove.getMovedPiece().getPoints());
            }
        }
        
        // If no attackers, piece is safe
        if (attackerValues.isEmpty()) {
            return true;
        }
        
        // If no defenders, piece is hanging
        if (defenderValues.isEmpty()) {
            return false;
        }
        
        // Sort attackers and defenders by value (cheapest first)
        attackerValues.sort(Integer::compareTo);
        defenderValues.sort(Integer::compareTo);
        
        // Simulate exchange sequence
        int currentPieceValue = pieceValue;
        boolean ourTurn = false; // Opponent attacks first
        int attackerIndex = 0;
        int defenderIndex = 0;
        int materialBalance = 0;
        
        while (attackerIndex < attackerValues.size() || defenderIndex < defenderValues.size()) {
            if (!ourTurn) { // Opponent's turn to attack
                if (attackerIndex < attackerValues.size()) {
                    materialBalance -= currentPieceValue; // We lose the piece
                    materialBalance += attackerValues.get(attackerIndex); // Opponent loses attacker when we recapture
                    currentPieceValue = attackerValues.get(attackerIndex); // Now defending with this piece
                    attackerIndex++;
                } else {
                    break; // No more attackers
                }
            } else { // Our turn to recapture
                if (defenderIndex < defenderValues.size()) {
                    materialBalance += currentPieceValue; // We capture the opponent piece
                    materialBalance -= defenderValues.get(defenderIndex); // We lose our defender
                    currentPieceValue = defenderValues.get(defenderIndex); // Now opponent recaptures with this
                    defenderIndex++;
                } else {
                    break; // No more defenders
                }
            }
            ourTurn = !ourTurn;
        }
        
        // For valuable pieces (3+ points), require at least break-even or better
        if (pieceValue >= 3) {
            System.out.println("PROTECTION ANALYSIS: " + pieceValue + " point piece, exchange result: " + materialBalance);
            return materialBalance >= -1; // Allow small loss but not major loss
        } else {
            return materialBalance >= 0; // For pawns, require break-even
        }
    }
    
    /**
     * DOOMED PIECE LOGIC METHODS
     */
    
    /**
     * Checks if a piece is valuable (worth 3+ points)
     */
    private boolean isValuablePiece(Piece piece) {
        return piece.getPoints() >= 3; // Bishop, Knight, Rook, Queen
    }
    
    /**
     * Checks if a valuable piece is doomed (will be captured no matter where it moves)
     */
    private boolean isPieceDoomed(Board board, Piece piece, Team team) {
        if (!isValuablePiece(piece)) {
            return false; // Only check valuable pieces
        }
        
        // Find current position of the piece
        int currentRow = -1, currentCol = -1;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board.getTile(i, j).getPiece() == piece) {
                    currentRow = i;
                    currentCol = j;
                    break;
                }
            }
        }
        
        if (currentRow == -1) return false; // Piece not found
        
        // Get all possible moves for this piece
        List<Move> pieceMoves = piece.availableMoves(board, new chess_game.Pieces.Coordinate(currentRow, currentCol));
        
        if (pieceMoves.isEmpty()) {
            return true; // No moves available - definitely doomed
        }
        
        // Check if ALL possible moves lead to the piece being captured
        int safeMoves = 0;
        int capturesMadeFromDoomedPosition = 0;
        
        for (Move move : pieceMoves) {
            Board testBoard = board.deepCopy();
            Player currentPlayer = testBoard.getCurrentPlayer();
            currentPlayer.makeMove(testBoard, move);
            
            // Check if piece is still hanging after this move
            if (!isPieceStillHanging(testBoard, move.getDestinationTile().getCoordinate(), team)) {
                safeMoves++;
            } else {
                // Piece will still be captured, but did it capture something?
                if (move.hasKilledPiece()) {
                    capturesMadeFromDoomedPosition++;
                }
            }
        }
        
        // Piece is doomed if:
        // 1. No safe moves available, OR
        // 2. Less than 20% of moves are safe (mostly doomed)
        boolean isDoomed = (safeMoves == 0) || (safeMoves < pieceMoves.size() * 0.2);
        
        return isDoomed;
    }

    /**
     * CHECKMATE DETECTION - Highest priority for AI
     */
    private Move findImmediateCheckmate(Board board, Team team) {
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, team);
        
        for (Move move : legalMoves) {
            Board testBoard = board.deepCopy();
            Player currentPlayer = testBoard.getCurrentPlayer();
            currentPlayer.makeMove(testBoard, move);
            
            String gameState = MoveUtilities.getGameState(testBoard, getOpponent(team));
            if ("CHECKMATE".equals(gameState)) {
                return move; // Found checkmate!
            }
        }
        
        return null; // No immediate checkmate available
    }
    
    /**
     * OPENING RANDOMIZATION - Prevents repetitive play
     */
    private boolean shouldRandomizeMove(Board board) {
        // Count total moves made (rough estimate)
        int piecesMoved = 0;
        int totalPieces = 0;
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null) {
                    totalPieces++;
                    // Rough heuristic: if pieces are not in starting positions
                    if (!isInStartingPosition(piece, i, j)) {
                        piecesMoved++;
                    }
                }
            }
        }
        
        // Randomize if less than 6 pieces have moved (opening phase)
        return piecesMoved < 6;
    }
    
    /**
     * Checks if a piece is in its starting position
     */
    private boolean isInStartingPosition(Piece piece, int row, int col) {
        String pieceType = piece.getClass().getSimpleName();
        Team team = piece.getTeam();
        
        switch (pieceType) {
            case "Pawn":
                return (team == Team.WHITE && row == 6) || (team == Team.BLACK && row == 1);
            case "Rook":
                return (team == Team.WHITE && row == 7 && (col == 0 || col == 7)) ||
                       (team == Team.BLACK && row == 0 && (col == 0 || col == 7));
            case "Knight":
                return (team == Team.WHITE && row == 7 && (col == 1 || col == 6)) ||
                       (team == Team.BLACK && row == 0 && (col == 1 || col == 6));
            case "Bishop":
                return (team == Team.WHITE && row == 7 && (col == 2 || col == 5)) ||
                       (team == Team.BLACK && row == 0 && (col == 2 || col == 5));
            case "Queen":
                return (team == Team.WHITE && row == 7 && col == 3) ||
                       (team == Team.BLACK && row == 0 && col == 3);
            case "King":
                return (team == Team.WHITE && row == 7 && col == 4) ||
                       (team == Team.BLACK && row == 0 && col == 4);
            default:
                return false;
        }
    }
    
    /**
     * Adds opening randomization to prevent repetitive play
     */
    private List<Move> addOpeningRandomization(List<Move> moves, Board board, Team team) {
        // Placeholder: shuffle moves for randomization
        java.util.Collections.shuffle(moves);
        return moves;
    }
    
    /**
     * Selects from top moves with slight randomization to avoid repetitive play
     */
    private Move selectFromTopMoves(List<MoveScore> sortedResults) {
        if (sortedResults.isEmpty()) return null;
        
        // If there's a clear best move (significantly better), take it
        double bestScore = sortedResults.get(0).score;
        
        if (sortedResults.size() == 1) {
            return sortedResults.get(0).move;
        }
        
        double secondBestScore = sortedResults.get(1).score;
        
        // If best move is significantly better (>50 points), always take it
        if (bestScore - secondBestScore > 50) {
            return sortedResults.get(0).move;
        }
        
        // Otherwise, add slight randomization among top moves
        int topMoveCount = 1;
        for (int i = 1; i < Math.min(3, sortedResults.size()); i++) {
            if (Math.abs(sortedResults.get(i).score - bestScore) < 10) {
                topMoveCount++;
            } else {
                break;
            }
        }
        
        // Randomly select from top moves (weighted towards best)
        java.util.Random random = new java.util.Random();
        int selection = random.nextInt(topMoveCount);
        
        return sortedResults.get(selection).move;
    }
    
    /**
     * CHECKMATE IN TWO MOVES DETECTION
     * Absolute priority - will find checkmate opportunities within 2 moves
     */
    private Move findCheckmateInTwoMoves(Board board, Team aiTeam) {
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, aiTeam);
        Team opponent = getOpponent(aiTeam);
        
        // First check for immediate checkmate (1 move)
        for (Move move : legalMoves) {
            Board testBoard = board.deepCopy();
            Player currentPlayer = testBoard.getCurrentPlayer();
            currentPlayer.makeMove(testBoard, move);
            
            if (MoveUtilities.isCheckmate(testBoard, opponent)) {
                return move; // Immediate checkmate found!
            }
        }
        
        // Then check for forced checkmate in 2 moves (optimized - limit search for performance)
        int maxMovesToCheck = Math.min(10, legalMoves.size()); // Only check top 10 moves for performance
        for (int i = 0; i < maxMovesToCheck; i++) {
            Move move = legalMoves.get(i);
            Board testBoard = board.deepCopy();
            Player currentPlayer = testBoard.getCurrentPlayer();
            currentPlayer.makeMove(testBoard, move);
            
            // Check if this move puts opponent in check
            if (MoveUtilities.controlCheckState(testBoard, opponent)) {
                // Get opponent's forced responses
                List<Move> opponentResponses = MoveUtilities.getLegalMoves(testBoard, opponent);
                
                boolean allResponsesLeadToCheckmate = true;
                
                // Limit opponent responses checked for performance
                int maxResponsesToCheck = Math.min(5, opponentResponses.size());
                for (int j = 0; j < maxResponsesToCheck; j++) {
                    Move opponentMove = opponentResponses.get(j);
                    Board testBoard2 = testBoard.deepCopy();
                    Player currentPlayer2 = testBoard2.getCurrentPlayer();
                    currentPlayer2.makeMove(testBoard2, opponentMove);
                    
                    // Check if we can checkmate after opponent's response
                    List<Move> ourFollowUps = MoveUtilities.getLegalMoves(testBoard2, aiTeam);
                    boolean canCheckmateAfterThisResponse = false;
                    
                    // Limit follow-up moves checked for performance
                    int maxFollowUpsToCheck = Math.min(8, ourFollowUps.size());
                    for (int k = 0; k < maxFollowUpsToCheck; k++) {
                        Move followUp = ourFollowUps.get(k);
                        Board testBoard3 = testBoard2.deepCopy();
                        Player currentPlayer3 = testBoard3.getCurrentPlayer();
                        currentPlayer3.makeMove(testBoard3, followUp);
                        
                        if (MoveUtilities.isCheckmate(testBoard3, opponent)) {
                            canCheckmateAfterThisResponse = true;
                            break;
                        }
                    }
                    
                    if (!canCheckmateAfterThisResponse) {
                        allResponsesLeadToCheckmate = false;
                        break;
                    }
                }
                
                if (allResponsesLeadToCheckmate && !opponentResponses.isEmpty()) {
                    return move; // This move leads to forced checkmate in 2!
                }
            }
        }
        
        return null; // No checkmate in 1-2 moves found
    }
    
    /**
     * FIND BEST FREE CAPTURE
     * Critical priority - finds completely free pieces that can be captured
     */
    private Move findBestFreeCapture(Board board, Team aiTeam) {
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, aiTeam);
        Move bestFreeCapture = null;
        int highestValue = 0;
        
        for (Move move : legalMoves) {
            if (move.hasKilledPiece()) {
                // Check if this is truly a free capture
                if (isCompleteFreeCapture(board, move, aiTeam)) {
                    int capturedValue = move.getKilledPiece().getPoints();
                    
                    if (capturedValue > highestValue) {
                        highestValue = capturedValue;
                        bestFreeCapture = move;
                    }
                }
            }
        }
        
        return bestFreeCapture;
    }
    
    /**
     * ENHANCED FREE CAPTURE DETECTION
     * More thorough than the original isFreeCapture method
     */
    private boolean isCompleteFreeCapture(Board board, Move captureMove, Team aiTeam) {
        Team opponent = getOpponent(aiTeam);
        
        // Simulate the capture
        Board testBoard = board.deepCopy();
        Player currentPlayer = testBoard.getCurrentPlayer();
        currentPlayer.makeMove(testBoard, captureMove);
        
        // Get destination coordinates
        int destRow = captureMove.getDestinationTile().getCoordinate().getX();
        int destCol = captureMove.getDestinationTile().getCoordinate().getY();
        
        // Check all opponent moves for potential recaptures
        List<Move> opponentMoves = MoveUtilities.getLegalMoves(testBoard, opponent);
        
        for (Move opponentMove : opponentMoves) {
            if (opponentMove.hasKilledPiece()) {
                int opponentDestRow = opponentMove.getDestinationTile().getCoordinate().getX();
                int opponentDestCol = opponentMove.getDestinationTile().getCoordinate().getY();
                
                // If opponent can recapture on the same square
                if (opponentDestRow == destRow && opponentDestCol == destCol) {
                    return false; // Not a free capture - opponent can recapture
                }
            }
        }
        
        return true; // Completely free capture!
    }
    
    /**
     * FIND BEST SAVE MOVE
     * Emergency priority - saves our most valuable hanging pieces
     */
    private Move findBestSaveMove(Board board, Team aiTeam) {
        List<Piece> hangingPieces = getHangingPieces(board, aiTeam);
        
        if (hangingPieces.isEmpty()) {
            return null; // No hanging pieces to save
        }
        
        // Sort hanging pieces by value (highest first)
        hangingPieces.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));
        
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, aiTeam);
        
        // Try to save the most valuable hanging piece first
        for (Piece hangingPiece : hangingPieces) {
            // Find retreat moves for this hanging piece
            for (Move move : legalMoves) {
                if (move.getMovedPiece() == hangingPiece) {
                    // Check if this move actually saves the piece
                    Board testBoard = board.deepCopy();
                    Player currentPlayer = testBoard.getCurrentPlayer();
                    currentPlayer.makeMove(testBoard, move);
                    
                    // Verify the piece is no longer hanging
                    if (!isPieceStillHanging(testBoard, move.getDestinationTile().getCoordinate(), aiTeam)) {
                        return move; // Found a save move!
                    }
                }
            }
            
            // If retreat isn't possible, try defending with another piece
            for (Move move : legalMoves) {
                if (move.getMovedPiece() != hangingPiece) {
                    Board testBoard = board.deepCopy();
                    Player currentPlayer = testBoard.getCurrentPlayer();
                    currentPlayer.makeMove(testBoard, move);
                    
                    // Check if this move defends the hanging piece
                    List<Piece> newHangingPieces = getHangingPieces(testBoard, aiTeam);
                    if (!newHangingPieces.contains(hangingPiece)) {
                        // Only accept efficient defenses
                        if (move.getMovedPiece().getPoints() < hangingPiece.getPoints()) {
                            return move; // Found efficient defense!
                        }
                    }
                }
            }
        }
        
        return null; // No good save moves found
    }

    /**
     * OPTIMIZED MATERIAL EVALUATION
     * Enhanced material counting with piece activity considerations
     */
    private double evaluateOptimizedMaterial(Board board, Team aiTeam, Team opponentTeam) {
        double ourMaterial = 0;
        double opponentMaterial = 0;
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null) {
                    double baseValue = piece.getPoints();
                    double activityBonus = calculatePieceActivity(piece, i, j);
                    double totalValue = baseValue + activityBonus;
                    
                    if (piece.getTeam() == aiTeam) {
                        ourMaterial += totalValue;
                    } else {
                        opponentMaterial += totalValue;
                    }
                }
            }
        }
        
        return ourMaterial - opponentMaterial;
    }
    
    /**
     * Calculate activity bonus for piece positioning
     */
    private double calculatePieceActivity(Piece piece, int row, int col) {
        String pieceType = piece.getClass().getSimpleName();
        double bonus = 0;
        
        switch (pieceType) {
            case "Knight":
                // Knights are better in center
                if (row >= 2 && row <= 5 && col >= 2 && col <= 5) {
                    bonus += 0.3;
                }
                break;
            case "Bishop":
                // Bishops prefer long diagonals
                bonus += Math.min(row, 7-row) * 0.1 + Math.min(col, 7-col) * 0.1;
                break;
            case "Pawn":
                // Pawns get bonus for advancement
                int advancement = (piece.getTeam() == Team.WHITE) ? 7 - row : row;
                bonus += advancement * 0.05;
                break;
        }
        
        return bonus;
    }
    
    /**
     * EFFICIENT THREAT EVALUATION
     * Streamlined threat analysis focusing on high-impact scenarios
     */
    private double evaluateEfficientThreats(Board board, Team aiTeam, Team opponentTeam) {
        double threatScore = 0;
        
        // 1. Quick hanging piece analysis
        List<Piece> ourHanging = getHangingPieces(board, aiTeam);
        List<Piece> theirHanging = getHangingPieces(board, opponentTeam);
        
        // MASSIVE penalty for our hanging pieces - AI must prioritize saving them
        for (Piece piece : ourHanging) {
            threatScore -= piece.getPoints() * 25; // Increased from 8 to 25
            // Extra penalty for valuable pieces
            if (piece.getPoints() >= 5) {
                threatScore -= piece.getPoints() * 10;
            }
            // Special queen penalty
            if (piece.getPoints() >= 9) {
                threatScore -= 150; // Extra penalty for hanging queen
            }
        }
        
        // Bonus for their hanging pieces
        for (Piece piece : theirHanging) {
            threatScore += piece.getPoints() * 6;
        }
        
        // 2. Immediate high-value captures
        List<Move> ourMoves = MoveUtilities.getLegalMoves(board, aiTeam);
        for (Move move : ourMoves) {
            if (move.hasKilledPiece()) {
                int capturedValue = move.getKilledPiece().getPoints();
                int attackerValue = move.getMovedPiece().getPoints();
                
                if (capturedValue > attackerValue) {
                    threatScore += (capturedValue - attackerValue) * 2;
                }
            }
        }
        
        return threatScore;
    }
    
    /**
     * STREAMLINED POSITIONAL FACTORS
     * Essential positional considerations without heavy computation
     */
    private double evaluatePositionalFactors(Board board, Team aiTeam, Team opponentTeam) {
        double positionalScore = 0;
        
        // 1. Center control (simplified)
        positionalScore += evaluateSimpleCenterControl(board, aiTeam, opponentTeam);
        
        // 2. King safety (basic)
        positionalScore += evaluateBasicKingSafety(board, aiTeam, opponentTeam);
        
        // 3. Piece mobility difference
        int ourMobility = MoveUtilities.getLegalMoves(board, aiTeam).size();
        int theirMobility = MoveUtilities.getLegalMoves(board, opponentTeam).size();
        positionalScore += (ourMobility - theirMobility) * 0.2;
        
        return positionalScore;
    }
    
    /**
     * Simple center control evaluation
     */
    private double evaluateSimpleCenterControl(Board board, Team aiTeam, Team opponentTeam) {
        double control = 0;
        
        // Check central squares (d4, e4, d5, e5)
        int[][] centerSquares = {{3,3}, {3,4}, {4,3}, {4,4}};
        
        for (int[] square : centerSquares) {
            Piece piece = board.getTile(square[0], square[1]).getPiece();
            if (piece != null) {
                if (piece.getTeam() == aiTeam) {
                    control += 2;
                } else {
                    control -= 2;
                }
            }
        }
        
        return control;
    }
    
    /**
     * Basic king safety evaluation
     */
    private double evaluateBasicKingSafety(Board board, Team aiTeam, Team opponentTeam) {
        double safety = 0;
        
        // Find kings and evaluate basic safety
        int[] ourKing = findKing(board, aiTeam);
        int[] theirKing = findKing(board, opponentTeam);
        
        if (ourKing != null) {
            // Penalty for exposed king in center
            if (ourKing[1] >= 2 && ourKing[1] <= 5) {
                safety -= 10;
            }
        }
        
        if (theirKing != null) {
            // Bonus for opponent's exposed king
            if (theirKing[1] >= 2 && theirKing[1] <= 5) {
                safety += 10;
            }
        }
        
        return safety;
    }
    
    /**
     * Find king position for a team
     */
    private int[] findKing(Board board, Team team) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null && piece.getClass().getSimpleName().equals("King") && piece.getTeam() == team) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }
    
    /**
     * SIMPLIFIED BOARD HASHING for better cache performance
     */
    private String getSimpleBoardHash(Board board) {
        StringBuilder hash = new StringBuilder(128);
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null) {
                    // Use first letter of piece type + team + position
                    hash.append(piece.getClass().getSimpleName().charAt(0))
                        .append(piece.getTeam() == Team.WHITE ? 'W' : 'B')
                        .append(i).append(j);
                }
            }
        }
        
        return hash.toString();
    }
    
    /**
     * EFFICIENT CACHE MANAGEMENT WITH PERIODIC CLEANUP
     */
    private void clearOldestCacheEntries() {
        long currentTime = System.currentTimeMillis();
        
        // Periodic cleanup every 30 seconds
        if (currentTime - lastCacheCleanup > CACHE_CLEANUP_INTERVAL) {
            performCacheCleanup();
            lastCacheCleanup = currentTime;
        }
        
        // Emergency cleanup if cache gets too large
        if (evaluationCache.size() > 15000) {
            performCacheCleanup();
        }
    }
    
    /**
     * Performs actual cache cleanup
     */
    private void performCacheCleanup() {
        // Clear 40% of evaluation cache
        if (evaluationCache.size() > 5000) {
            int targetSize = (int)(evaluationCache.size() * 0.6);
            java.util.Iterator<String> iterator = evaluationCache.keySet().iterator();
            
            while (evaluationCache.size() > targetSize && iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        
        // Note: Removed hanging piece and material cache for simplified implementation
    }
    
    /**
     * Cache a move for a given position
     */
    private void cacheMove(String boardHash, Team team, Move move) {
        if (bestMoveCache.size() < MAX_CACHE_SIZE) {
            bestMoveCache.put(boardHash + team.toString(), move);
        }
    }
    
    /**
     * Periodic cache cleanup to maintain performance
     */
    private void performPeriodicCacheCleanup() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheCleanup > CACHE_CLEANUP_INTERVAL) {
            if (evaluationCache.size() > MAX_CACHE_SIZE) {
                evaluationCache.clear();
            }
            if (bestMoveCache.size() > MAX_CACHE_SIZE) {
                bestMoveCache.clear();
            }
            lastCacheCleanup = currentTime;
        }
    }
    
    /**
     * Get cache statistics for debugging
     */
    public String getCacheStats() {
        return String.format("Cache Stats - Hits: %d, Misses: %d, Hit Rate: %.2f%%, Eval Cache Size: %d, Move Cache Size: %d",
            cacheHits, cacheMisses, 
            cacheMisses > 0 ? (cacheHits * 100.0 / (cacheHits + cacheMisses)) : 0.0,
            evaluationCache.size(), bestMoveCache.size());
    }

    private static class MoveScore {
        Move move;
        double score;

        MoveScore(Move move, double score) {
            this.move = move;
            this.score = score;
        }
    }

    /**
     * CRITICAL QUEEN RESCUE LOGIC
     * Highest priority after checkmate - queen is worth 9 points!
     */
    private Move findCriticalQueenRescue(Board board, Team team) {
        // Find our queen
        Piece queen = null;
        int queenRow = -1, queenCol = -1;
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null && piece.getTeam() == team && 
                    piece.getClass().getSimpleName().equals("Queen")) {
                    queen = piece;
                    queenRow = i;
                    queenCol = j;
                    break;
                }
            }
        }
        
        if (queen == null) return null; // No queen to rescue
        
        // Check if queen is hanging
        Team opponent = getOpponent(team);
        List<Move> opponentMoves = MoveUtilities.getLegalMoves(board, opponent);
        
        boolean queenIsHanging = false;
        Move threatMove = null;
        
        for (Move opponentMove : opponentMoves) {
            if (opponentMove.hasKilledPiece() && opponentMove.getKilledPiece() == queen) {
                // Check if this is truly a hanging queen (no adequate defense)
                if (isQueenReallyHanging(board, opponentMove, team)) {
                    queenIsHanging = true;
                    threatMove = opponentMove;
                    break;
                }
            }
        }
        
        if (!queenIsHanging) return null; // Queen is safe
        
        System.out.println("CRITICAL: Queen is hanging! Finding rescue move...");
        
        // PRIORITY 1: Try to move the queen to safety
        List<Move> queenMoves = queen.availableMoves(board, new chess_game.Pieces.Coordinate(queenRow, queenCol));
        for (Move queenMove : queenMoves) {
            Board testBoard = board.deepCopy();
            Player currentPlayer = testBoard.getCurrentPlayer();
            currentPlayer.makeMove(testBoard, queenMove);
            
            // Check if queen is safe after this move
            if (!isPieceStillHanging(testBoard, queenMove.getDestinationTile().getCoordinate(), team)) {
                System.out.println("CRITICAL: Found queen escape move!");
                return queenMove; // Found escape!
            }
        }
        
        // PRIORITY 2: Try to eliminate the threat (capture the threatening piece)
        if (threatMove != null) {
            Piece threateningPiece = threatMove.getMovedPiece();
            List<Move> ourMoves = MoveUtilities.getLegalMoves(board, team);
            
            for (Move move : ourMoves) {
                if (move.hasKilledPiece() && move.getKilledPiece() == threateningPiece) {
                    // Check if this capture is safe or worth it
                    if (move.getMovedPiece().getPoints() <= threateningPiece.getPoints()) {
                        System.out.println("CRITICAL: Found move to eliminate queen threat!");
                        return move; // Eliminate the threat
                    }
                }
            }
        }
        
        // PRIORITY 3: Try to block the threat
        Move blockMove = findQueenBlockMove(board, threatMove, team);
        if (blockMove != null) {
            System.out.println("CRITICAL: Found move to block queen threat!");
            return blockMove;
        }
        
        // PRIORITY 4: If queen is doomed, try to trade it for maximum value
        Move tradeMove = findBestQueenTrade(board, team);
        if (tradeMove != null) {
            System.out.println("CRITICAL: Queen is doomed - making best trade!");
            return tradeMove;
        }
        
        return null; // No rescue possible
    }

    /**
     * VALUE-BASED RESCUE LOGIC
     * Prioritizes saving pieces by their point value
     */
    private Move findValueBasedRescue(Board board, Team team) {
        List<Piece> hangingPieces = getHangingPieces(board, team);
        
        if (hangingPieces.isEmpty()) {
            return null; // No hanging pieces to save
        }
        
        // Sort hanging pieces by value (highest first) - CRITICAL for piece priority
        hangingPieces.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));
        
        System.out.println("VALUE-BASED RESCUE: Found " + hangingPieces.size() + " hanging pieces");
        
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, team);
        
        // Try to save the most valuable hanging piece first
        for (Piece hangingPiece : hangingPieces) {
            System.out.println("Trying to save hanging " + hangingPiece.getClass().getSimpleName() + 
                             " worth " + hangingPiece.getPoints() + " points");
            
            // PRIORITY 1: Try to move the hanging piece to safety
            for (Move move : legalMoves) {
                if (move.getMovedPiece() == hangingPiece) {
                    // Check if this move actually saves the piece
                    Board testBoard = board.deepCopy();
                    Player currentPlayer = testBoard.getCurrentPlayer();
                    currentPlayer.makeMove(testBoard, move);
                    
                    // Verify the piece is no longer hanging
                    if (!isPieceStillHanging(testBoard, move.getDestinationTile().getCoordinate(), team)) {
                        System.out.println("VALUE-BASED RESCUE: Found retreat move for " + 
                                         hangingPiece.getClass().getSimpleName());
                        return move; // Found a safe retreat!
                    }
                }
            }
            
            // PRIORITY 2: Try efficient defense (defend with less valuable piece)
            for (Move move : legalMoves) {
                if (move.getMovedPiece() != hangingPiece) {
                    Board testBoard = board.deepCopy();
                    Player currentPlayer = testBoard.getCurrentPlayer();
                    currentPlayer.makeMove(testBoard, move);
                    
                    // Check if this move defends the hanging piece
                    List<Piece> newHangingPieces = getHangingPieces(testBoard, team);
                    if (!newHangingPieces.contains(hangingPiece)) {
                        // Check if defense is efficient (defender worth less than defended)
                        if (move.getMovedPiece().getPoints() < hangingPiece.getPoints()) {
                            System.out.println("VALUE-BASED RESCUE: Found efficient defense for " + 
                                             hangingPiece.getClass().getSimpleName() + 
                                             " using " + move.getMovedPiece().getClass().getSimpleName());
                            return move; // Found efficient defense!
                        }
                    }
                }
            }
        }
        
        System.out.println("VALUE-BASED RESCUE: No efficient rescue found");
        return null; // No efficient rescue moves found
    }

    /**
     * EFFICIENT TRADE-OFF LOGIC
     * When pieces are doomed, trade them for maximum value
     */
    private Move findEfficientTradeOff(Board board, Team team) {
        List<Piece> hangingPieces = getHangingPieces(board, team);
        
        if (hangingPieces.isEmpty()) {
            return null; // No hanging pieces
        }
        
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, team);
        Move bestTradeOff = null;
        int bestTradeValue = 0;
        
        System.out.println("EFFICIENT TRADE-OFF: Analyzing doomed pieces...");
        
        // For each hanging piece, see if it can capture something valuable before dying
        for (Piece hangingPiece : hangingPieces) {
            System.out.println("Analyzing trade options for doomed " + 
                             hangingPiece.getClass().getSimpleName() + 
                             " worth " + hangingPiece.getPoints() + " points");
            
            for (Move move : legalMoves) {
                if (move.getMovedPiece() == hangingPiece && move.hasKilledPiece()) {
                    int capturedValue = move.getKilledPiece().getPoints();
                    int doomedPieceValue = hangingPiece.getPoints();
                    
                    // Calculate trade efficiency
                    int tradeValue = capturedValue - doomedPieceValue;
                    
                    // Prioritize trades that minimize our loss or maximize our gain
                    if (capturedValue >= doomedPieceValue || // Break even or better
                        tradeValue > bestTradeValue) { // Better than current best trade
                        
                        // Verify the piece will still be captured after this move
                        Board testBoard = board.deepCopy();
                        Player currentPlayer = testBoard.getCurrentPlayer();
                        currentPlayer.makeMove(testBoard, move);
                        
                        // If piece is still hanging after the trade, it's a valid trade-off
                        if (isPieceStillHanging(testBoard, move.getDestinationTile().getCoordinate(), team) ||
                            capturedValue >= doomedPieceValue) {
                            
                            bestTradeValue = tradeValue;
                            bestTradeOff = move;
                            
                            System.out.println("EFFICIENT TRADE-OFF: Found trade - " +
                                             hangingPiece.getClass().getSimpleName() + 
                                             " captures " + move.getKilledPiece().getClass().getSimpleName() +
                                             " (net: " + tradeValue + " points)");
                        }
                    }
                }
            }
        }
        
        if (bestTradeOff != null) {
            System.out.println("EFFICIENT TRADE-OFF: Selected best trade with value " + bestTradeValue);
        } else {
            System.out.println("EFFICIENT TRADE-OFF: No beneficial trades found");
        }
        
        return bestTradeOff;
    }
    
    /**
     * Checks if queen is really hanging (more thorough than standard hanging check)
     */
    private boolean isQueenReallyHanging(Board board, Move threatMove, Team team) {
        Piece queen = threatMove.getKilledPiece();
        Piece attacker = threatMove.getMovedPiece();
        
        // If attacker is worth less than queen, it's definitely hanging
        if (attacker.getPoints() < queen.getPoints()) {
            // Check if we can adequately defend
            Board testBoard = board.deepCopy();
            Player currentPlayer = testBoard.getCurrentPlayer();
            currentPlayer.makeMove(testBoard, threatMove);
            
            // Check for recapture possibilities
            List<Move> ourMoves = MoveUtilities.getLegalMoves(testBoard, team);
            for (Move recapture : ourMoves) {
                if (recapture.hasKilledPiece() && recapture.getKilledPiece() == attacker) {
                    // Check if recapture is with a piece worth less than queen
                    if (recapture.getMovedPiece().getPoints() <= attacker.getPoints()) {
                        return false; // Queen can be adequately defended
                    }
                }
            }
            return true; // Queen is really hanging
        }
        
        return false; // Equal or higher value trade - not considered "hanging"
    }
    
    /**
     * Finds a move to block the threat to the queen
     */
    private Move findQueenBlockMove(Board board, Move threatMove, Team team) {
        if (threatMove == null) return null;
        
        // Get the path from threatening piece to queen
        int threatRow = threatMove.getCurrentTile().getCoordinate().getX();
        int threatCol = threatMove.getCurrentTile().getCoordinate().getY();
        int queenRow = threatMove.getDestinationTile().getCoordinate().getX();
        int queenCol = threatMove.getDestinationTile().getCoordinate().getY();
        
        // Calculate direction
        int rowDir = Integer.compare(queenRow, threatRow);
        int colDir = Integer.compare(queenCol, threatCol);
        
        // Find squares between threat and queen that can be blocked
        List<chess_game.Pieces.Coordinate> blockSquares = new java.util.ArrayList<>();
        int currentRow = threatRow + rowDir;
        int currentCol = threatCol + colDir;
        
        while (currentRow != queenRow || currentCol != queenCol) {
            blockSquares.add(new chess_game.Pieces.Coordinate(currentRow, currentCol));
            currentRow += rowDir;
            currentCol += colDir;
        }
        
        // Try to find a piece that can move to one of these blocking squares
        List<Move> ourMoves = MoveUtilities.getLegalMoves(board, team);
        for (Move move : ourMoves) {
            for (chess_game.Pieces.Coordinate blockSquare : blockSquares) {
                int destRow = move.getDestinationTile().getCoordinate().getX();
                int destCol = move.getDestinationTile().getCoordinate().getY();
                
                if (destRow == blockSquare.getX() && destCol == blockSquare.getY()) {
                    // Check if this piece can safely block
                    if (move.getMovedPiece().getPoints() < 9) { // Don't block with queen itself
                        return move; // Found blocking move
                    }
                }
            }
        }
        
        return null; // No blocking move found
    }
    
    /**
     * Finds the best trade for a doomed queen
     */
    private Move findBestQueenTrade(Board board, Team team) {
        // Find our queen
        Piece queen = null;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null && piece.getTeam() == team && 
                    piece.getClass().getSimpleName().equals("Queen")) {
                    queen = piece;
                    break;
                }
            }
        }
        
        if (queen == null) return null;
        
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, team);
        Move bestTrade = null;
        int bestCaptureValue = 0;
        
        // Find the highest value piece the queen can capture
        for (Move move : legalMoves) {
            if (move.getMovedPiece() == queen && move.hasKilledPiece()) {
                int captureValue = move.getKilledPiece().getPoints();
                if (captureValue > bestCaptureValue) {
                    bestCaptureValue = captureValue;
                    bestTrade = move;
                }
            }
        }
        
        return bestTrade;
    }
    
    /**
     * CRITICAL: Check if a move creates a hanging piece
     * This is essential to prevent the AI from making poor moves
     * ENHANCED VERSION: Stricter safety checks for valuable pieces
     */
    private boolean createHangingPiece(Move move, Board board, Team team) {
        // Simulate the move
        Board testBoard = board.deepCopy();
        Player currentPlayer = testBoard.getCurrentPlayer();
        currentPlayer.makeMove(testBoard, move);
        
        // Get the destination coordinates where our piece will be
        int destRow = move.getDestinationTile().getCoordinate().getX();
        int destCol = move.getDestinationTile().getCoordinate().getY();
        
        // Switch to opponent's turn to check if they can capture our piece
        testBoard.changeCurrentPlayer();
        Team opponent = getOpponent(team);
        List<Move> opponentMoves = MoveUtilities.getLegalMoves(testBoard, opponent);
        
        // Check if opponent can capture our piece for free or unfavorably
        for (Move opponentMove : opponentMoves) {
            if (opponentMove.hasKilledPiece()) {
                int captureRow = opponentMove.getDestinationTile().getCoordinate().getX();
                int captureCol = opponentMove.getDestinationTile().getCoordinate().getY();
                
                // If opponent can capture the piece we just moved
                if (captureRow == destRow && captureCol == destCol) {
                    Piece capturedPiece = opponentMove.getKilledPiece();
                    Piece capturingPiece = opponentMove.getMovedPiece();
                    
                    // ENHANCED: More stringent checks for valuable pieces
                    int ourPieceValue = capturedPiece.getPoints();
                    int theirPieceValue = capturingPiece.getPoints();
                    
                    // For bishops, knights, rooks, queens - be extra careful
                    if (ourPieceValue >= 3) {
                        // ANY unfavorable or equal trade is considered hanging for valuable pieces
                        if (ourPieceValue >= theirPieceValue) {
                            // Check if we have adequate protection
                            testBoard.changeCurrentPlayer(); // Switch back to our turn
                            
                            // ENHANCED: More thorough protection check
                            if (!hasAdequateProtection(testBoard, destRow, destCol, team, ourPieceValue)) {
                                System.out.println("ENHANCED HANGING DETECTION: " + 
                                                 move.getMovedPiece().getClass().getSimpleName() + 
                                                 " (" + ourPieceValue + " pts) would be hanging to " +
                                                 capturingPiece.getClass().getSimpleName() + 
                                                 " (" + theirPieceValue + " pts) at [" + destRow + ", " + destCol + "]!");
                                return true; // This move creates a hanging piece
                            }
                            testBoard.changeCurrentPlayer(); // Switch back for next iteration
                        }
                    } else {
                        // For pawns, use original logic (only bad trades)
                        if (ourPieceValue > theirPieceValue) {
                            testBoard.changeCurrentPlayer(); // Switch back to our turn
                            if (!isPieceProtected(testBoard, destRow, destCol, team)) {
                                System.out.println("HANGING DETECTION: " + 
                                                 move.getMovedPiece().getClass().getSimpleName() + 
                                                 " would be hanging at [" + destRow + ", " + destCol + "]!");
                                return true;
                            }
                            testBoard.changeCurrentPlayer();
                        }
                    }
                }
            }
        }
        
        return false; // Move doesn't create a hanging piece
    }
    
    /**
     * SIMPLIFIED OPPONENT QUEEN CAPTURE
     * Only captures the queen if it's completely free - other queen captures will be handled by general move evaluation
     */
    private Move findOpponentQueenCapture(Board board, Team aiTeam) {
        Team opponent = getOpponent(aiTeam);
        
        // Find opponent's queen
        Piece opponentQueen = null;
        int queenRow = -1, queenCol = -1;
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece piece = board.getTile(i, j).getPiece();
                if (piece != null && piece.getTeam() == opponent && 
                    piece.getClass().getSimpleName().equals("Queen")) {
                    opponentQueen = piece;
                    queenRow = i;
                    queenCol = j;
                    break;
                }
            }
        }
        
        if (opponentQueen == null) {
            return null; // No opponent queen to capture
        }
        
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, aiTeam);
        
        // ONLY look for FREE queen captures (no retaliation)
        for (Move move : legalMoves) {
            if (move.hasKilledPiece() && move.getKilledPiece() == opponentQueen) {
                if (isCompleteFreeCapture(board, move, aiTeam)) {
                    System.out.println("QUEEN HUNTER: Found FREE queen capture with " + 
                                     move.getMovedPiece().getClass().getSimpleName() + "!");
                    return move; // FREE QUEEN CAPTURE!
                }
            }
        }
        
        return null; // No free queen capture found - let general evaluation handle other queen captures
    }
    
    /**
     * Calculates the net value of capturing opponent's queen
     */
    private int calculateQueenCaptureValue(Board board, Move queenCaptureMove, Team aiTeam) {
        Board testBoard = board.deepCopy();
        Player currentPlayer = testBoard.getCurrentPlayer();
        currentPlayer.makeMove(testBoard, queenCaptureMove);
        
        int queenValue = 9; // Queen worth 9 points
        int attackerValue = queenCaptureMove.getMovedPiece().getPoints();
        
        // Check if opponent can immediately recapture
        Team opponent = getOpponent(aiTeam);
        List<Move> opponentMoves = MoveUtilities.getLegalMoves(testBoard, opponent);
        
        int destRow = queenCaptureMove.getDestinationTile().getCoordinate().getX();
        int destCol = queenCaptureMove.getDestinationTile().getCoordinate().getY();
        
        // Find cheapest recapture
        int cheapestRecapture = Integer.MAX_VALUE;
        boolean canRecapture = false;
        
        for (Move opponentMove : opponentMoves) {
            if (opponentMove.hasKilledPiece()) {
                int opponentDestRow = opponentMove.getDestinationTile().getCoordinate().getX();
                int opponentDestCol = opponentMove.getDestinationTile().getCoordinate().getY();
                
                if (opponentDestRow == destRow && opponentDestCol == destCol) {
                    canRecapture = true;
                    int recaptureValue = opponentMove.getKilledPiece().getPoints();
                    if (recaptureValue == attackerValue) {
                        cheapestRecapture = Math.min(cheapestRecapture, 
                                                   opponentMove.getMovedPiece().getPoints());
                    }
                }
            }
        }
        
        if (!canRecapture) {
            return queenValue; // Free queen capture!
        } else {
            // Calculate net material gain
            int netGain = queenValue - attackerValue;
            if (cheapestRecapture != Integer.MAX_VALUE) {
                netGain -= cheapestRecapture; // We lose our piece, they lose recapturing piece
            }
            return netGain;
        }
    }
    
    /**
     * Finds opportunities to attack or trap the opponent's queen
     */
    private Move findQueenAttackOpportunity(Board board, Team aiTeam, Piece opponentQueen) {
        // Find queen position
        int queenRow = -1, queenCol = -1;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (board.getTile(i, j).getPiece() == opponentQueen) {
                    queenRow = i;
                    queenCol = j;
                    break;
                }
            }
        }
        
        if (queenRow == -1) return null;
        
        List<Move> legalMoves = MoveUtilities.getLegalMoves(board, aiTeam);
        
        // Look for moves that attack the queen (force it to move)
        for (Move move : legalMoves) {
            Board testBoard = board.deepCopy();
            Player currentPlayer = testBoard.getCurrentPlayer();
            currentPlayer.makeMove(testBoard, move);
            
            // Check if our move attacks the queen square
            List<Move> ourNewMoves = MoveUtilities.getLegalMoves(testBoard, aiTeam);
            for (Move attackMove : ourNewMoves) {
                int destRow = attackMove.getDestinationTile().getCoordinate().getX();
                int destCol = attackMove.getDestinationTile().getCoordinate().getY();
                
                if (destRow == queenRow && destCol == queenCol) {
                    // This move creates an attack on the queen!
                    // Check if it's a good move (doesn't hang our piece badly)
                    if (move.getMovedPiece().getPoints() <= 5) { // Don't sacrifice queen to attack queen
                        // Check if this move creates a hanging piece
                        if (createHangingPiece(move, board, aiTeam)) {
                            System.out.println("QUEEN HUNTER: Skipping queen attack that hangs " +
                                             move.getMovedPiece().getClass().getSimpleName());
                            continue; // Skip moves that hang the attacking piece
                        }
                        System.out.println("QUEEN HUNTER: Move creates queen attack with " +
                                         move.getMovedPiece().getClass().getSimpleName());
                        return move;
                    }
                }
            }
        }
        
        return null; // No queen attack opportunities found
    }
}
