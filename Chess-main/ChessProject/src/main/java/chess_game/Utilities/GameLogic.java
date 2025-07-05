package chess_game.Utilities;

import chess_game.Boards.Board;
import chess_game.Move.Move;
import chess_game.Pieces.Coordinate;
import chess_game.Pieces.Piece;
import chess_game.Pieces.PieceTypes;
import chess_game.Pieces.Team;
import chess_game.Player.Player;
import chess_game.gui.BoardPanel;
import chess_game.gui.InGameBottomMenu;
import chess_game.gui.Table;
import chess_game.ChessAI;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import java.awt.Color;

/**
 * Shared game logic for both AI and multiplayer modes
 * Reduces code duplication and centralizes move handling
 */
public class GameLogic {
    
    /**
     * Handle move execution for both AI and multiplayer modes
     */
    public static boolean executeMove(Board board, Move move, BoardPanel boardPanel, 
                                    InGameBottomMenu bottomMenu, boolean isAIMode) {
        if (move == null) return false;
        
        try {
            // Track captured pieces before making the move
            if (move.hasKilledPiece() && bottomMenu != null) {
                bottomMenu.addCapturedPiece(
                    move.getKilledPiece().toString(),
                    move.getKilledPiece().getPoints()
                );
            }
            
            // Execute the move
            board.getCurrentPlayer().makeMove(board, move);
            
            // Update board display
            if (boardPanel != null) {
                boardPanel.updateBoardGUI(board);
            }
            
            // Switch players
            board.changeCurrentPlayer();
            
            // Update UI for current player's turn
            updateTurnDisplay(board, bottomMenu, isAIMode);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Create a promotion move with user choice
     */
    public static Move createPromotionMove(Board board, Coordinate from, Coordinate to, 
                                         Team team, BoardPanel boardPanel, boolean isAIMode) {
        Move move = new Move(board, board.getTile(from), board.getTile(to));
        
        // Check if this is actually a promotion
        Piece piece = board.getTile(from).getPiece();
        if (piece != null && piece.getType() == PieceTypes.PAWN) {
            boolean isPromotionRank = (team == Team.WHITE && to.getY() == 0) ||
                                    (team == Team.BLACK && to.getY() == 7);
            
            if (isPromotionRank) {
                move.setPromotionMove(true);
                
                // Handle promotion piece selection
                if (isAIMode && team == Team.BLACK) {
                    // AI always promotes to queen
                    move.setPromotionPieceType(PieceTypes.QUEEN);
                } else {
                    // Human player - show dialog
                    PieceTypes promotionType = showPromotionDialog(boardPanel);
                    if (promotionType == null) {
                        return null; // User cancelled
                    }
                    move.setPromotionPieceType(promotionType);
                }
            }
        }
        
        return move;
    }
    
    /**
     * Show promotion dialog and return selected piece type
     */
    private static PieceTypes showPromotionDialog(BoardPanel boardPanel) {
        String[] options = {"Queen", "Rook", "Bishop", "Knight"};
        int choice = JOptionPane.showOptionDialog(
            boardPanel,
            "Choose piece for promotion:",
            "Pawn Promotion",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        if (choice == JOptionPane.CLOSED_OPTION) {
            return null; // User cancelled
        }
        
        switch (choice) {
            case 0: return PieceTypes.QUEEN;
            case 1: return PieceTypes.ROOK;
            case 2: return PieceTypes.BISHOP;
            case 3: return PieceTypes.KNIGHT;
            default: return PieceTypes.QUEEN;
        }
    }
    
    /**
     * Check for game ending conditions
     */
    public static boolean checkGameEnd(Board board, Table table, boolean isAIMode) {
        String gameState = MoveUtilities.getGameState(board, board.getCurrentPlayer().getTeam());
        
        if (gameState != null) {
            if ("CHECKMATE".equals(gameState)) {
                Team winnerTeam = (board.getCurrentPlayer().getTeam() == Team.BLACK) ? Team.WHITE : Team.BLACK;
                String winnerName;
                
                if (isAIMode) {
                    winnerName = (winnerTeam == Team.WHITE) ? table.getPlayerName() : "AI";
                } else {
                    winnerName = winnerTeam.toString() + " player";
                }
                
                JOptionPane.showMessageDialog(table.getGameFrame(),
                    "Checkmate! " + winnerName + " wins!",
                    "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
                
                table.createMainMenu();
                return true;
                
            } else if ("STALEMATE".equals(gameState)) {
                JOptionPane.showMessageDialog(table.getGameFrame(),
                    "Stalemate! The game is a draw.",
                    "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
                
                table.createMainMenu();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Update turn display in the UI
     */
    private static void updateTurnDisplay(Board board, InGameBottomMenu bottomMenu, boolean isAIMode) {
        if (bottomMenu == null) return;
        
        Team currentTeam = board.getCurrentPlayer().getTeam();
        
        if (isAIMode) {
            if (currentTeam == Team.WHITE) {
                bottomMenu.getTurnLBL().setText("Your Turn");
                bottomMenu.getTurnLBL().setForeground(Color.GREEN);
            } else {
                bottomMenu.getTurnLBL().setText("AI Turn");
                bottomMenu.getTurnLBL().setForeground(Color.RED);
            }
        } else {
            // Multiplayer mode - show current team
            bottomMenu.getTurnLBL().setText(currentTeam.toString() + "'s Turn");
            bottomMenu.getTurnLBL().setForeground(currentTeam == Team.WHITE ? Color.GREEN : Color.RED);
        }
    }
    
    /**
     * Execute AI move in background thread
     */
    public static void executeAIMove(Board board, Team aiTeam, BoardPanel boardPanel, 
                                   InGameBottomMenu bottomMenu, Table table) {
        SwingWorker<Move, Void> aiWorker = new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() throws Exception {
                ChessAI ai = new ChessAI(3); // Configurable depth
                return ai.getBestMove(board, aiTeam);
            }
            
            @Override
            protected void done() {
                try {
                    Move aiMove = get();
                    if (aiMove != null) {
                        executeMove(board, aiMove, boardPanel, bottomMenu, true);
                        checkGameEnd(board, table, true);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        
        aiWorker.execute();
    }
    
    /**
     * Validate if it's the correct player's turn
     */
    public static boolean isValidTurn(Board board, Team playerTeam, boolean isAIMode, boolean isPaired) {
        if (isAIMode) {
            // In AI mode, only allow moves when it's the human player's turn
            return board.getCurrentPlayer().getTeam() == playerTeam;
        } else {
            // In multiplayer mode, check pairing and turn
            return isPaired && board.getCurrentPlayer().getTeam() == playerTeam;
        }
    }
    
    /**
     * Create castling move
     */
    public static Move createCastlingMove(Board board, Coordinate kingFrom, Coordinate kingTo) {
        int dx = kingTo.getX() - kingFrom.getX();
        int dy = kingTo.getY() - kingFrom.getY();
        
        if (Math.abs(dx) == 2 && dy == 0) {
            int y = kingFrom.getY();
            int rookStartX = dx > 0 ? 7 : 0; // Kingside=7, Queenside=0
            int rookEndX = dx > 0 ? kingFrom.getX() + 1 : kingFrom.getX() - 1;
            
            return new Move(board, 
                          board.getTile(kingFrom), 
                          board.getTile(kingTo),
                          board.getTile(rookStartX, y), 
                          board.getTile(rookEndX, y));
        }
        
        return null;
    }
    
    /**
     * Check if a move is a castling move
     */
    public static boolean isCastlingMove(Coordinate from, Coordinate to, PieceTypes pieceType) {
        if (pieceType != PieceTypes.KING) return false;
        
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        
        return Math.abs(dx) == 2 && dy == 0;
    }
    
    /**
     * Save game state
     */
    public static boolean saveGame(Board board, String playerName, String opponentName, 
                                 String saveName, boolean isAIMode) {
        try {
            Messages.GameState gameState = new Messages.GameState(
                board,
                playerName,
                isAIMode ? "AI" : opponentName,
                board.getCurrentPlayer().getTeam(),
                saveName
            );
            
            return GameStateManager.saveGame(gameState);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
