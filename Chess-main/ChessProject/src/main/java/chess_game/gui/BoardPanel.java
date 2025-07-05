/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess_game.gui;

import ClientSide.Client;
import chess_game.Boards.Board;
import chess_game.Pieces.Coordinate;
import chess_game.Pieces.Team;
import javax.swing.JPanel;
import chess_game.Resources.BOARD_Configurations;
import java.awt.GridLayout;
import java.awt.Dimension;

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
}
