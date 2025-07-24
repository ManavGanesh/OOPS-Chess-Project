/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess_game.Move;

import chess_game.Boards.Board;
import chess_game.Boards.Tile;
import chess_game.Pieces.Piece;
import chess_game.Pieces.PieceTypes;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */
public class Move implements java.io.Serializable{

    private static final long serialVersionUID = 1L;
    
    Board board;
    Tile currentTile;
    Tile destinationTile;
    Piece movedPiece;
    Piece killedPiece;
    
    // Castling support
    private boolean isCastlingMove = false;
    private Tile rookStartTile;
    private Tile rookEndTile;

    // En passant support
    private boolean isEnPassantMove = false;
    private Tile enPassantCapturedTile;

    // Promotion support
    private boolean isPromotionMove = false;
    private PieceTypes promotionPieceType = null;

    public Move(Board board, Tile currentTile, Tile destinationTile) {
        this.board = board;
        this.currentTile = currentTile;
        this.destinationTile = destinationTile;
        this.movedPiece = currentTile.getPiece();
        if (destinationTile.hasPiece()) {
            killedPiece = destinationTile.getPiece();
        }
        this.isCastlingMove = false;
        this.isEnPassantMove = false;
    }

    // Constructor for castling move
    public Move(Board board, Tile kingStartTile, Tile kingEndTile, Tile rookStartTile, Tile rookEndTile) {
        this.board = board;
        this.currentTile = kingStartTile;
        this.destinationTile = kingEndTile;
        this.movedPiece = kingStartTile.getPiece();
        this.rookStartTile = rookStartTile;
        this.rookEndTile = rookEndTile;
        this.isCastlingMove = true;
    }

    // Constructor for en passant
    public Move(Board board, Tile currentTile, Tile destinationTile, Tile enPassantCapturedTile) {
        this.board = board;
        this.currentTile = currentTile;
        this.destinationTile = destinationTile;
        this.movedPiece = currentTile.getPiece();
        this.enPassantCapturedTile = enPassantCapturedTile;
        this.isEnPassantMove = true;
        this.isCastlingMove = false;
        this.killedPiece = enPassantCapturedTile.getPiece();
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    public Tile getCurrentTile() {
        return currentTile;
    }

    public void setCurrentTile(Tile currentTile) {
        this.currentTile = currentTile;
    }

    public Tile getDestinationTile() {
        return destinationTile;
    }

    public void setDestinationTile(Tile destinationTile) {
        this.destinationTile = destinationTile;
    }

    public Piece getMovedPiece() {
        return movedPiece;
    }

    public void setMovedPiece(Piece movedPiece) {
        this.movedPiece = movedPiece;
    }

    public Piece getKilledPiece() {
        return killedPiece;
    }

    public void setKilledPiece(Piece killedPiece) {
        this.killedPiece = killedPiece;
    }
    
    public boolean hasKilledPiece()
    {
        return this.killedPiece != null;
    }

    public boolean isCastlingMove() {
        return isCastlingMove;
    }

    public Tile getRookStartTile() {
        return rookStartTile;
    }

    public Tile getRookEndTile() {
        return rookEndTile;
    }

    public boolean isEnPassantMove() {
        return isEnPassantMove;
    }

    public Tile getEnPassantCapturedTile() {
        return enPassantCapturedTile;
    }

    public boolean isPromotionMove() {
        return isPromotionMove;
    }

    public PieceTypes getPromotionPieceType() {
        return promotionPieceType;
    }

    public void setPromotionMove(boolean isPromotionMove) {
        this.isPromotionMove = isPromotionMove;
    }

    public void setPromotionPieceType(PieceTypes promotionPieceType) {
        this.promotionPieceType = promotionPieceType;
    }
    
    /**
     * Generate a notation string for this move for learning purposes
     */
    public String getNotation() {
        StringBuilder notation = new StringBuilder();
        
        // Add piece type
        if (movedPiece != null) {
            notation.append(movedPiece.getClass().getSimpleName().charAt(0));
        }
        
        // Add current position
        if (currentTile != null && currentTile.getCoordinate() != null) {
            notation.append(currentTile.getCoordinate().getX())
                   .append(currentTile.getCoordinate().getY());
        }
        
        // Add capture indicator
        if (hasKilledPiece()) {
            notation.append("x");
        } else {
            notation.append("-");
        }
        
        // Add destination position
        if (destinationTile != null && destinationTile.getCoordinate() != null) {
            notation.append(destinationTile.getCoordinate().getX())
                   .append(destinationTile.getCoordinate().getY());
        }
        
        // Add special move indicators
        if (isCastlingMove) {
            notation.append("O");
        }
        if (isEnPassantMove) {
            notation.append("e.p.");
        }
        if (isPromotionMove) {
            notation.append("=").append(promotionPieceType != null ? promotionPieceType.name().charAt(0) : 'Q');
        }
        
        return notation.toString();
    }
}
