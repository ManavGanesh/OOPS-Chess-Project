/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess_game.Boards;

import chess_game.Pieces.*;
import chess_game.Player.Player;
import chess_game.Resources.*;
import chess_game.Utilities.BoardUtilities;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */

//The purpose of this class is keeping the tiles (which are empty or keeping a piece). Actually this class
// is the abstract board that pieces are sit on. Not have visualization. 
//The all board you see is the visualized version of this board.

public class Board implements java.io.Serializable{

    private final Tile[][] tiles;
    private Player whitePlayer;
    private Player blackPlayer;
    private Player currentPlayer;
    private Tile chosenTile = null;
    private Tile enPassantTargetTile = null;
    private chess_game.Move.Move lastMove = null;

    public Player getWhitePlayer() {
        return whitePlayer;
    }

    public Player getBlackPlayer() {
        return blackPlayer;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public Tile getChosenTile() {
        return chosenTile;
    }

    public boolean hasChosenTile() {
        boolean hasChosen = false;
        if(chosenTile == null)
        {
            return false;
        }
        if(chosenTile.getPiece() == null)
        {
            return false;   
        }
        return true;
    }

    public void setChosenTile(Tile chosenTile) {
        if (chosenTile == null || !chosenTile.hasPiece()) {
            this.chosenTile = null;
        } else {
            this.chosenTile = chosenTile;
        }
    }

    public Tile getTile(Coordinate coordinate) {
        return getTile(coordinate.getX(), coordinate.getY());
    }

    public Board() {
        //black
        whitePlayer = new Player(Team.WHITE);
        blackPlayer = new Player(Team.BLACK);
        currentPlayer = whitePlayer;
        tiles = BoardUtilities.createStandartBoardTiles();

    }

    public Tile getTile(int x, int y) {
        if (x < 0 || x > 7 || y < 0 || y > 7) {
            System.out.println("Get Tile Index Bound Of Array");
            return null;
        }
        return tiles[x][y];
    }

    public Coordinate getCoordOfGivenTeamPiece(Team team, PieceTypes pieceType) {
        for (int i = 0; i < BOARD_Configurations.ROW_COUNT; i++) {
            for (int j = 0; j < BOARD_Configurations.ROW_TILE_COUNT; j++) {
                if (!tiles[i][j].hasPiece()) {
                    continue;
                }
                if (tiles[i][j].getPiece().getTeam() == team && tiles[i][j].getPiece().getType() == pieceType) {
                    return tiles[i][j].getCoordinate();
                }
            }
        }
        return null;
    }

    public void changeCurrentPlayer() {
        if (currentPlayer == whitePlayer) {
            currentPlayer = blackPlayer;
        } else {
            currentPlayer = whitePlayer;
        }
    }

    public Tile getTileOfGivenTeamPiece(Team team, PieceTypes pieceType) {
        for (int i = 0; i < BOARD_Configurations.ROW_COUNT; i++) {
            for (int j = 0; j < BOARD_Configurations.ROW_TILE_COUNT; j++) {
                if (!tiles[i][j].hasPiece()) {
                    continue;
                }
                if (tiles[i][j].getPiece().getTeam() == team && tiles[i][j].getPiece().getType() == pieceType) {
                    return tiles[i][j];
                }
            }
        }
        return null;
    }

    public Tile getEnPassantTargetTile() {
        return enPassantTargetTile;
    }

    public void setEnPassantTargetTile(Tile enPassantTargetTile) {
        this.enPassantTargetTile = enPassantTargetTile;
    }

    public chess_game.Move.Move getLastMove() {
        return lastMove;
    }

    public void setLastMove(chess_game.Move.Move lastMove) {
        this.lastMove = lastMove;
    }

    public Board deepCopy() {
        Board copy = new Board();
        // Copy tiles and pieces
        for (int i = 0; i < BOARD_Configurations.ROW_COUNT; i++) {
            for (int j = 0; j < BOARD_Configurations.ROW_TILE_COUNT; j++) {
                Tile originalTile = this.getTile(i, j);
                Tile copiedTile = copy.getTile(i, j);
                if (originalTile.hasPiece()) {
                    copiedTile.setPiece(originalTile.getPiece().clone());
                } else {
                    copiedTile.setPiece(null);
                }
            }
        }
        // Copy chosen tile if any
        if (this.chosenTile != null && this.chosenTile.hasPiece()) {
            Coordinate coord = this.chosenTile.getCoordinate();
            copy.chosenTile = copy.getTile(coord);
        }
        // Copy en passant target tile if any
        if (this.enPassantTargetTile != null) {
            Coordinate coord = this.enPassantTargetTile.getCoordinate();
            copy.enPassantTargetTile = copy.getTile(coord);
        }
        // Copy last move if any (shallow copy is fine for simulation)
        copy.lastMove = this.lastMove;
        // Copy players and current player
        copy.whitePlayer = new chess_game.Player.Player(Team.WHITE);
        copy.blackPlayer = new chess_game.Player.Player(Team.BLACK);
        copy.currentPlayer = (this.currentPlayer.getTeam() == Team.WHITE) ? copy.whitePlayer : copy.blackPlayer;
        return copy;
    }
}
