/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess_game.Player;

import chess_game.Move.Move;
import chess_game.Boards.Board;
import chess_game.Pieces.*;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */

//The player which is making moves on board. They have team also black or white.
public class Player implements java.io.Serializable{
    
    private Team team;
    
    public Player(Team team)
    {
        this.team = team;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
    
    public void makeMove(Board board, Move move)
    {
        if (move.isCastlingMove()) {
            // Move the king
            board.getTile(move.getDestinationTile().getCoordinate()).setPiece(move.getCurrentTile().getPiece());
            board.getTile(move.getCurrentTile().getCoordinate()).setPiece(null);
            // Move the rook
            board.getTile(move.getRookEndTile().getCoordinate()).setPiece(move.getRookStartTile().getPiece());
            board.getTile(move.getRookStartTile().getCoordinate()).setPiece(null);
            // Set hasMoved for king and rook
            Piece king = board.getTile(move.getDestinationTile().getCoordinate()).getPiece();
            Piece rook = board.getTile(move.getRookEndTile().getCoordinate()).getPiece();
            if (king instanceof King) {
                ((King) king).setHasMoved(true);
            }
            if (rook instanceof Rook) {
                ((Rook) rook).setHasMoved(true);
            }
        } else if (move.isEnPassantMove()) {
            // Move the pawn
            board.getTile(move.getDestinationTile().getCoordinate()).setPiece(move.getCurrentTile().getPiece());
            board.getTile(move.getCurrentTile().getCoordinate()).setPiece(null);
            // Remove the captured pawn
            board.getTile(move.getEnPassantCapturedTile().getCoordinate()).setPiece(null);
        } else if (move.isPromotionMove()) {
            // Handle pawn promotion
            Piece promotionPiece = createPromotionPiece(move.getPromotionPieceType(), move.getMovedPiece().getTeam());
            board.getTile(move.getDestinationTile().getCoordinate()).setPiece(promotionPiece);
            board.getTile(move.getCurrentTile().getCoordinate()).setPiece(null);
        } else {
            board.getTile(move.getDestinationTile().getCoordinate()).setPiece(move.getCurrentTile().getPiece());
            board.getTile(move.getCurrentTile().getCoordinate()).setPiece(null);
            // Set hasMoved for king or rook
            Piece moved = board.getTile(move.getDestinationTile().getCoordinate()).getPiece();
            if (moved instanceof King) {
                ((King) moved).setHasMoved(true);
            }
            if (moved instanceof Rook) {
                ((Rook) moved).setHasMoved(true);
            }
        }
        // Track last move for en passant
        board.setLastMove(move);
    }
    
    private Piece createPromotionPiece(PieceTypes pieceType, Team team) {
        switch (pieceType) {
            case QUEEN:
                return new Queen(team);
            case ROOK:
                return new Rook(team);
            case BISHOP:
                return new Bishop(team);
            case KNIGHT:
                return new Knight(team);
            default:
                return new Queen(team); // Default to queen
        }
    }
}
