/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess_game.gui;

import ClientSide.Client;
import chess_game.Boards.Board;
import chess_game.Boards.Tile;
import chess_game.Pieces.Coordinate;
import chess_game.Pieces.Piece;
import chess_game.Pieces.Team;
import chess_game.Move.Move;
import chess_game.Utilities.MoveUtilities;
import javax.swing.JPanel;
import chess_game.Resources.BOARD_Configurations;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */

// This class is the visual version of the Board object.
public class BoardPanel extends JPanel {

    private TilePanel boardTiles[][];
    private Client client;
    private boolean isBoardRotated;
    private Table table; // Reference to the main table for AI mode
    private Team playerTeam; // Store player team for AI mode

    public BoardPanel(Board chessBoard, Client client) {
        super(new GridLayout(BOARD_Configurations.ROW_COUNT, BOARD_Configurations.ROW_TILE_COUNT));
        this.client = client;
        this.isBoardRotated = (client != null && client.getTeam() == Team.BLACK);
        this.boardTiles = new TilePanel[BOARD_Configurations.ROW_COUNT][BOARD_Configurations.ROW_TILE_COUNT];
        
        for (int i = 0; i < BOARD_Configurations.ROW_COUNT; i++) {
            for (int j = 0; j < BOARD_Configurations.ROW_TILE_COUNT; j++) {
                Coordinate displayCoord = getDisplayCoordinate(i, j);
                TilePanel tilePanel = new TilePanel(this, displayCoord, chessBoard, client);
                this.boardTiles[i][j] = tilePanel;
                add(tilePanel);
            }
        }
        this.revalidate();
        this.repaint();
    }
    
    /**
     * Constructor for AI mode with player team selection
     * @param chessBoard The chess board
     * @param client The client (null for AI mode)
     * @param playerTeam The player's team (WHITE or BLACK)
     */
    public BoardPanel(Board chessBoard, Client client, Team playerTeam) {
        super(new GridLayout(BOARD_Configurations.ROW_COUNT, BOARD_Configurations.ROW_TILE_COUNT));
        this.client = client;
        this.playerTeam = playerTeam;
        
        // In AI mode, rotate board if player chose BLACK so their pieces face them
        if (client == null) {
            // AI mode - rotate based on player team
            this.isBoardRotated = (playerTeam == Team.BLACK);
        } else {
            // Multiplayer mode - use existing logic
            this.isBoardRotated = (client.getTeam() == Team.BLACK);
        }
        
        this.boardTiles = new TilePanel[BOARD_Configurations.ROW_COUNT][BOARD_Configurations.ROW_TILE_COUNT];
        
        for (int i = 0; i < BOARD_Configurations.ROW_COUNT; i++) {
            for (int j = 0; j < BOARD_Configurations.ROW_TILE_COUNT; j++) {
                Coordinate displayCoord = getDisplayCoordinate(i, j);
                TilePanel tilePanel = new TilePanel(this, displayCoord, chessBoard, client);
                this.boardTiles[i][j] = tilePanel;
                add(tilePanel);
            }
        }
        this.revalidate();
        this.repaint();
    }

    /**
     * Converts display coordinates to actual board coordinates based on player's team
     * @param row The display row (0-7)
     * @param col The display column (0-7)
     * @return The actual board coordinate
     */
    private Coordinate getDisplayCoordinate(int row, int col) {
        if (isBoardRotated) {
            // For black player, rotate the board 180 degrees
            // This means (0,0) becomes (7,7), (0,1) becomes (7,6), etc.
            return new Coordinate(7 - col, 7 - row);
        } else {
            // For white player, keep the original orientation
            return new Coordinate(col, row);
        }
    }

    /**
     * Converts actual board coordinates to display coordinates based on player's team
     * @param coord The actual board coordinate
     * @return The display coordinate
     */
    public Coordinate getDisplayCoordinate(Coordinate coord) {
        if (isBoardRotated) {
            // For black player, rotate the board 180 degrees
            return new Coordinate(7 - coord.getX(), 7 - coord.getY());
        } else {
            // For white player, keep the original orientation
            return coord;
        }
    }

    public TilePanel[][] getBoardTiles() {
        return boardTiles;
    }

    public void setBoardTiles(TilePanel[][] boardTiles) {
        this.boardTiles = boardTiles;
    }

    public void updateBoardGUI(Board board) {
        for (int i = 0; i < BOARD_Configurations.ROW_COUNT; i++) {
            for (int j = 0; j < BOARD_Configurations.ROW_TILE_COUNT; j++) {
                boardTiles[i][j].assignTileColor(board);
                boardTiles[i][j].assignTilePieceIcon(board);
            }
        }
        
        // MOVE PREVIEW SYSTEM: Clear move indicators unless a piece is currently selected
        if (!board.hasChosenTile()) {
            clearAllMoveIndicators();
        }
    }

    public boolean isBoardRotated() {
        return isBoardRotated;
    }
    
    public Table getTable() {
        return table;
    }
    
    public void setTable(Table table) {
        this.table = table;
    }
    
    public Team getPlayerTeam() {
        return playerTeam;
    }

    @Override
    public Dimension getPreferredSize() {
        // Fetch parent window size
        Dimension screenSize = getParent().getSize();

        // Calculate usable width and height (subtract any fixed UI like menus)
        int usableWidth = screenSize.width;
        int usableHeight = screenSize.height-20; // subtract header/menu bar height

        // Use the smaller of width or height to preserve square shape
        int boardSize = Math.min(usableWidth, usableHeight);

        // Cap the maximum size
        int maxBoardSize = 1000; // Max size in pixels
        boardSize = Math.min(boardSize, maxBoardSize);

        return new Dimension(boardSize, boardSize);
    }
    
    /**
     * MOVE PREVIEW SYSTEM: Show legal moves for a selected piece
     * @param selectedTile The tile containing the piece whose moves to show
     */
    public void showLegalMovesForPiece(Tile selectedTile) {
        if (selectedTile == null || !selectedTile.hasPiece()) {
            return;
        }
        
        // We need to get the current board from the boardTiles since Tile doesn't have board reference
        // Let's find the current board by looking for a tile panel that matches
        Board tempBoard = null;
        for (int i = 0; i < BOARD_Configurations.ROW_COUNT && tempBoard == null; i++) {
            for (int j = 0; j < BOARD_Configurations.ROW_TILE_COUNT; j++) {
                if (boardTiles[i][j] != null) {
                    tempBoard = boardTiles[i][j].chessBoard;
                    break;
                }
            }
        }
        
        if (tempBoard == null) {
            return;
        }
        
        // Make a final reference for use in lambda expressions
        final Board currentBoard = tempBoard;
        
        Piece piece = selectedTile.getPiece();
        Coordinate pieceCoord = selectedTile.getCoordinate();
        
        // Get all legal moves for the piece directly from MoveUtilities
        // This ensures castling moves are properly validated
        List<Move> allLegalMoves = MoveUtilities.getLegalMoves(currentBoard, piece.getTeam());
        
        // Filter to only show moves for the selected piece
        List<Move> legalMoves = new ArrayList<>();
        for (Move move : allLegalMoves) {
            if (move.getCurrentTile().getCoordinate().equals(pieceCoord)) {
                legalMoves.add(move);
            }
        }
        
        // Show move indicators for each legal move
        for (Move move : legalMoves) {
            Coordinate destCoord = move.getDestinationTile().getCoordinate();
            TilePanel destTilePanel = getTilePanelAt(destCoord);
            
            if (destTilePanel != null) {
                // Check if the piece would be safe after moving to this destination
                boolean isSafeMove = isMoveDestinationSafe(move, currentBoard);
                destTilePanel.showMoveIndicator(isSafeMove);
            }
        }
    }
    
    /**
     * MOVE PREVIEW SYSTEM: Clear all move indicators from the board
     */
    public void clearAllMoveIndicators() {
        for (int i = 0; i < BOARD_Configurations.ROW_COUNT; i++) {
            for (int j = 0; j < BOARD_Configurations.ROW_TILE_COUNT; j++) {
                if (boardTiles[i][j] != null) {
                    boardTiles[i][j].hideMoveIndicator();
                }
            }
        }
    }
    
    /**
     * MOVE PREVIEW SYSTEM: Get the TilePanel at the specified coordinate
     * @param coord The board coordinate
     * @return The TilePanel at that coordinate, or null if not found
     */
    private TilePanel getTilePanelAt(Coordinate coord) {
        for (int i = 0; i < BOARD_Configurations.ROW_COUNT; i++) {
            for (int j = 0; j < BOARD_Configurations.ROW_TILE_COUNT; j++) {
                if (boardTiles[i][j] != null && boardTiles[i][j].getCoordinate().equals(coord)) {
                    return boardTiles[i][j];
                }
            }
        }
        return null;
    }
    
    /**
     * MOVE PREVIEW SYSTEM: Check if a piece would be safe after moving to a destination
     * @param move The move to check
     * @param board The current board state
     * @return true if the piece would be safe, false if it would be under attack
     */
    private boolean isMoveDestinationSafe(Move move, Board board) {
        // For castling moves, always consider them safe since the castling logic
        // already checks that the king doesn't pass through or end up in check
        if (move.isCastlingMove()) {
            return true;
        }
        
        // Create a temporary board to simulate the move
        Board tempBoard = board.deepCopy();
        
        // Execute the move on the temporary board
        try {
            // Create a proper move for the temporary board
            Move tempMove;
            if (move.isEnPassantMove()) {
                tempMove = new Move(tempBoard, 
                    tempBoard.getTile(move.getCurrentTile().getCoordinate()),
                    tempBoard.getTile(move.getDestinationTile().getCoordinate()),
                    tempBoard.getTile(move.getEnPassantCapturedTile().getCoordinate()));
            } else if (move.isPromotionMove()) {
                tempMove = new Move(tempBoard, 
                    tempBoard.getTile(move.getCurrentTile().getCoordinate()),
                    tempBoard.getTile(move.getDestinationTile().getCoordinate()));
                tempMove.setPromotionMove(true);
                tempMove.setPromotionPieceType(move.getPromotionPieceType());
            } else {
                tempMove = new Move(tempBoard, 
                    tempBoard.getTile(move.getCurrentTile().getCoordinate()),
                    tempBoard.getTile(move.getDestinationTile().getCoordinate()));
            }
            
            tempBoard.getCurrentPlayer().makeMove(tempBoard, tempMove);
            
            // Switch to the opponent's turn
            tempBoard.changeCurrentPlayer();
            
            // Get the destination coordinate where our piece now sits
            Coordinate destCoord = move.getDestinationTile().getCoordinate();
            
            // Check if any opponent piece can attack this square
            Team opponentTeam = tempBoard.getCurrentPlayer().getTeam();
            List<Move> opponentMoves = MoveUtilities.getLegalMoves(tempBoard, opponentTeam);
            
            for (Move opponentMove : opponentMoves) {
                if (opponentMove.hasKilledPiece() && 
                    opponentMove.getDestinationTile().getCoordinate().equals(destCoord)) {
                    // The opponent can capture our piece at this destination
                    return false;
                }
            }
            
            return true; // No opponent can attack this square
        } catch (Exception e) {
            // If there's an error simulating the move, assume it's unsafe
            System.err.println("Error checking move safety: " + e.getMessage());
            return false;
        }
    }
}
