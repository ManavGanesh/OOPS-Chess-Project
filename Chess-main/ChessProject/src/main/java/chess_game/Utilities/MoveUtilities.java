/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess_game.Utilities;

import chess_game.Boards.Board;
import chess_game.Boards.Tile;
import chess_game.Pieces.Coordinate;
import chess_game.Move.Move;
import chess_game.Pieces.PieceTypes;
import chess_game.Pieces.Team;
import chess_game.Resources.PIECE_Configurations;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */
public class MoveUtilities {

    public static List<Move> getLegalMoves(Board board, Team team) {
        List<Move> legalMoves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Tile tile = board.getTile(i, j);
                if (tile.hasPiece() && tile.getPiece().getTeam() == team) {
                    for (Move move : tile.getPiece().availableMoves(board, tile.getCoordinate())) {
                        Board boardCopy = board.deepCopy();
                        Tile fromTile = boardCopy.getTile(tile.getCoordinate());
                        Tile toTile = boardCopy.getTile(move.getDestinationTile().getCoordinate());
                        
                        // Create the appropriate move type for simulation
                        Move simulationMove;
                        if (move.isCastlingMove()) {
                            Tile rookStartTile = boardCopy.getTile(move.getRookStartTile().getCoordinate());
                            Tile rookEndTile = boardCopy.getTile(move.getRookEndTile().getCoordinate());
                            simulationMove = new Move(boardCopy, fromTile, toTile, rookStartTile, rookEndTile);
                        } else if (move.isEnPassantMove()) {
                            Tile enPassantCapturedTile = boardCopy.getTile(move.getEnPassantCapturedTile().getCoordinate());
                            simulationMove = new Move(boardCopy, fromTile, toTile, enPassantCapturedTile);
                        } else if (move.isPromotionMove()) {
                            simulationMove = new Move(boardCopy, fromTile, toTile);
                            simulationMove.setPromotionMove(true);
                            simulationMove.setPromotionPieceType(move.getPromotionPieceType());
                        } else {
                            simulationMove = new Move(boardCopy, fromTile, toTile);
                        }
                        
                        // Simulate the move
                        boardCopy.getCurrentPlayer().makeMove(boardCopy, simulationMove);
                        // If king is not in check after the move, it's legal
                        if (!controlCheckState(boardCopy, team)) {
                            legalMoves.add(move);
                        }
                    }
                }
            }
        }
        return legalMoves;
    }

    public static boolean isValidMove(Board board, Tile destinationTile) {
        if (!board.hasChosenTile()) {
            return false;
        }
        Team team = board.getCurrentPlayer().getTeam();
        List<Move> legalMoves = getLegalMoves(board, team);
        for (Move move : legalMoves) {
            if (move.getCurrentTile().getCoordinate().equals(board.getChosenTile().getCoordinate()) &&
                move.getDestinationTile().getCoordinate().equals(destinationTile.getCoordinate())) {
                return true;
            }
        }
        return false;
    }

    public static boolean controlCheckState(Board board, Team team) {

        Tile destinationTile;
        Coordinate currentCoord = board.getCoordOfGivenTeamPiece(team, PieceTypes.KING);
        
        // Check if king exists (should always exist in a valid game)
        if (currentCoord == null) {
            return false; // No king found, can't be in check
        }
        
        //control is there a knight as a danger for king ( check state),
        for (Coordinate coord : PIECE_Configurations.KNIGHT_MOVES) {

            if (!BoardUtilities.isValidCoordinate(currentCoord.plus(coord))) {
                continue; // chech if the coord outside of board.
            }
            destinationTile = board.getTile(currentCoord.plus(coord));

            if (!destinationTile.hasPiece()) {
                continue;
            } else {
                if (destinationTile.getPiece().getTeam() != team && destinationTile.getPiece().getType() == PieceTypes.KNIGHT) {
                    return true;
                }
            }
        }
        
        //control is there a rooks, queen etc. (check danger from straight way)
        
        Tile currentTile = board.getTile(currentCoord);
        Coordinate destinationCoordinate;
        for (Coordinate coord : PIECE_Configurations.ROOK_MOVES) {
            destinationCoordinate = currentCoord;
            while (BoardUtilities.isValidCoordinate(destinationCoordinate.plus(coord))) {
                destinationCoordinate = destinationCoordinate.plus(coord);
                destinationTile = board.getTile(destinationCoordinate);
                if (!destinationTile.hasPiece()) {
                    continue;
                } else {
                    if (destinationTile.getPiece().getTeam() == team) {
                        break;
                    }
                    if (destinationTile.getPiece().getTeam() != team && (destinationTile.getPiece().getType() == PieceTypes.ROOK || destinationTile.getPiece().getType() == PieceTypes.QUEEN)) {
                        return true;
                    } else {
                        break;
                    }
                }
            }
        }
        //control is there a bishop, queen ( check danger from cross way)
        for (Coordinate coord : PIECE_Configurations.BISHOP_MOVES) {
            destinationCoordinate = currentCoord;
            while (BoardUtilities.isValidCoordinate(destinationCoordinate.plus(coord))) {
                destinationCoordinate = destinationCoordinate.plus(coord);
                destinationTile = board.getTile(destinationCoordinate);
                if (!destinationTile.hasPiece()) {
                    continue;
                } else {
                    if (destinationTile.getPiece().getTeam() == team) {
                        break;
                    }
                    if (destinationTile.getPiece().getTeam() != team && (destinationTile.getPiece().getType() == PieceTypes.BISHOP || destinationTile.getPiece().getType() == PieceTypes.QUEEN)) {
                        return true;

                    } else {
                        break;
                    }
                }
            }
        }
        
        // control is there a pawn that able to attack king nearby. 
        for (Coordinate coord : (Coordinate[]) PIECE_Configurations.PAWN_MOVES.get(team).get("Attack")) {

            if (!BoardUtilities.isValidCoordinate(currentCoord.plus(coord))) {
                continue;
            }
            destinationTile = board.getTile(currentCoord.plus(coord));

            if (!destinationTile.hasPiece()) {
                continue;
            } else {
                if (destinationTile.getPiece().getTeam() != team && destinationTile.getPiece().getType() == PieceTypes.PAWN) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a team is in checkmate
     * @param board The current board state
     * @param team The team to check for checkmate
     * @return true if the team is in checkmate, false otherwise
     */
    public static boolean isCheckmate(Board board, Team team) {
        // First check if the king is in check
        if (!controlCheckState(board, team)) {
            return false; // Not in check, so not checkmate
        }
        
        // Then check if there are any legal moves to get out of check
        List<Move> legalMoves = getLegalMoves(board, team);
        return legalMoves.isEmpty();
    }

    /**
     * Check if a team is in stalemate
     * @param board The current board state
     * @param team The team to check for stalemate
     * @return true if the team is in stalemate, false otherwise
     */
    public static boolean isStalemate(Board board, Team team) {
        // First check if the king is in check
        if (controlCheckState(board, team)) {
            return false; // In check, so not stalemate (would be checkmate if no moves)
        }
        
        // Then check if there are any legal moves
        List<Move> legalMoves = getLegalMoves(board, team);
        return legalMoves.isEmpty();
    }

    /**
     * Check if the game is over (checkmate or stalemate)
     * @param board The current board state
     * @param team The team to check
     * @return "CHECKMATE" if checkmate, "STALEMATE" if stalemate, null if game continues
     */
    public static String getGameState(Board board, Team team) {
        if (isCheckmate(board, team)) {
            return "CHECKMATE";
        } else if (isStalemate(board, team)) {
            return "STALEMATE";
        }
        return null; // Game continues
    }

}
