/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess_game.Pieces;

import chess_game.Boards.Board;
import chess_game.Move.Move;
import chess_game.Boards.Tile;
import java.util.ArrayList;
import java.util.List;
import chess_game.Resources.PIECE_Configurations;
import chess_game.Utilities.BoardUtilities;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */
public class Pawn extends Piece {

    public Pawn(Team team) {
        super(team, PieceTypes.PAWN);
    }

    @Override
    public List<Move> availableMoves(Board board, Coordinate currentCoord) {
        List<Move> possibleMoves = new ArrayList<Move>();
        Tile currentTile = board.getTile(currentCoord);
        Tile destinationTile;

        // We'll check for promotion when creating moves below

        //normal available moves calculating. Movement of 1 length on y or -y axis.
        for (Coordinate coord : (Coordinate[]) PIECE_Configurations.PAWN_MOVES.get(this.getTeam()).get("Normal")) {
            if (!BoardUtilities.isValidCoordinate(currentCoord.plus(coord))) {
                continue;
            }
            destinationTile = board.getTile(currentCoord.plus(coord));
            if (!destinationTile.hasPiece()) {
                Move move = new Move(board, currentTile, destinationTile);
                // Check if this move reaches the promotion rank
                boolean isPromotionRank = (this.getTeam() == Team.WHITE && destinationTile.getCoordinate().getY() == 0) ||
                                         (this.getTeam() == Team.BLACK && destinationTile.getCoordinate().getY() == 7);
                if (isPromotionRank) {
                    move.setPromotionMove(true);
                    move.setPromotionPieceType(PieceTypes.QUEEN); // Default to queen, can be changed by player
                }
                possibleMoves.add(move);
            }
            //not need to else state. becuse if there is a piece in any team on pawn it cant moves.             
        }
        if (currentTile.getCoordinate().getY() == PIECE_Configurations.getPawnStartPosY(this.getTeam())) {
            for (Coordinate coord : (Coordinate[]) PIECE_Configurations.PAWN_MOVES.get(this.getTeam()).get("Start")) {
                if (!BoardUtilities.isValidCoordinate(currentCoord.plus(coord))) {
                    continue;
                }
                destinationTile = board.getTile(currentCoord.plus(coord));
                if (!destinationTile.hasPiece()) {
                    possibleMoves.add(new Move(board, currentTile, destinationTile));
                }

            }
        }
        for (Coordinate coord : (Coordinate[]) PIECE_Configurations.PAWN_MOVES.get(this.getTeam()).get("Attack")) {

            if (!BoardUtilities.isValidCoordinate(currentCoord.plus(coord))) {
                continue;
            }
            destinationTile = board.getTile(currentCoord.plus(coord));

            if (!destinationTile.hasPiece()) {
                continue;
            } else {
                if (destinationTile.getPiece().getTeam() != this.getTeam()) {
                    Move move = new Move(board, currentTile, destinationTile);
                    // Check if this attack move reaches the promotion rank
                    boolean isAttackPromotionRank = (this.getTeam() == Team.WHITE && destinationTile.getCoordinate().getY() == 0) ||
                                                   (this.getTeam() == Team.BLACK && destinationTile.getCoordinate().getY() == 7);
                    if (isAttackPromotionRank) {
                        move.setPromotionMove(true);
                        move.setPromotionPieceType(PieceTypes.QUEEN); // Default to queen, can be changed by player
                    }
                    possibleMoves.add(move);
                }
            }
        }

        // En passant
        Board b = board;
        int direction = (this.getTeam() == Team.WHITE) ? -1 : 1;
        int y = currentCoord.getY();
        int x = currentCoord.getX();
        // Check left and right for en passant
        for (int dx = -1; dx <= 1; dx += 2) {
            int nx = x + dx;
            int ny = y;
            if (nx < 0 || nx > 7) continue;
            Tile adjacentTile = board.getTile(nx, ny);
            if (adjacentTile.hasPiece() && adjacentTile.getPiece().getType() == PieceTypes.PAWN && adjacentTile.getPiece().getTeam() != this.getTeam()) {
                // Check if this pawn just moved two squares
                Move lastMove = board.getLastMove();
                if (lastMove != null && lastMove.getMovedPiece() == adjacentTile.getPiece() && Math.abs(lastMove.getCurrentTile().getCoordinate().getY() - lastMove.getDestinationTile().getCoordinate().getY()) == 2) {
                    // The en passant target square is behind the enemy pawn
                    int targetY = y + direction;
                    if (targetY >= 0 && targetY <= 7 && !board.getTile(nx, targetY).hasPiece()) {
                        Tile enPassantTarget = board.getTile(nx, targetY);
                        possibleMoves.add(new Move(board, currentTile, enPassantTarget, adjacentTile));
                    }
                }
            }
        }

        return possibleMoves;
    }

    @Override
    public Piece clone() {
        Pawn copy = new Pawn(this.getTeam());
        copy.setKilled(this.isKilled());
        return copy;
    }

}
