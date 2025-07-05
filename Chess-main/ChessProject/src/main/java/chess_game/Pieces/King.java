/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess_game.Pieces;


import chess_game.Boards.Board;
import chess_game.Move.Move;
import chess_game.Boards.Tile;
import chess_game.Resources.PIECE_Configurations;
import chess_game.Utilities.BoardUtilities;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */
public class King extends Piece {

    private boolean castlingDone = false;
    private boolean hasMoved = false;

    public boolean isCastlingDone() {
        return castlingDone;
    }

    public void setCastlingDone(boolean castlingDone) {
        this.castlingDone = castlingDone;
    }

    public boolean hasMoved() {
        return hasMoved;
    }

    public void setHasMoved(boolean hasMoved) {
        this.hasMoved = hasMoved;
    }

    public King(Team team) {
        super(team, PieceTypes.KING);
        this.hasMoved = false;
    }

    @Override
    public List<Move> availableMoves(Board board, Coordinate currentCoord) {
        List<Move> possibleMoves = new ArrayList<Move>();
        Tile currentTile = board.getTile(currentCoord);
        Tile destinationTile;
        Coordinate destinationCoordinate;
        for (Coordinate coord : PIECE_Configurations.QUUEN_MOVES) {
            destinationCoordinate = currentCoord.plus(coord);
            if(!BoardUtilities.isValidCoordinate(destinationCoordinate))
            {
                continue;
            }
            destinationTile = board.getTile(destinationCoordinate);
            if (!destinationTile.hasPiece()) {
                possibleMoves.add(new Move(board, currentTile, destinationTile));
            } else {
                if (destinationTile.getPiece().getTeam() != this.getTeam()) {
                    possibleMoves.add(new Move(board, currentTile, destinationTile));
                }
            }
        }

        // --- Castling Logic ---
        if (!this.hasMoved() && !chess_game.Utilities.MoveUtilities.controlCheckState(board, this.getTeam())) {
            int y = currentCoord.getY();
            // Kingside castling (to the right, X increases)
            if (canCastle(board, currentCoord, true)) {
                // King moves two squares to the right (X+2)
                Coordinate kingSideCastle = new Coordinate(currentCoord.getX() + 2, currentCoord.getY());
                Tile rookStartTile = board.getTile(7, y); // Rook at X=7
                Tile rookEndTile = board.getTile(currentCoord.getX() + 1, y); // Rook moves to X+1
                possibleMoves.add(new Move(board, currentTile, board.getTile(kingSideCastle), rookStartTile, rookEndTile));
            }
            // Queenside castling (to the left, X decreases)
            if (canCastle(board, currentCoord, false)) {
                // King moves two squares to the left (X-2)
                Coordinate queenSideCastle = new Coordinate(currentCoord.getX() - 2, currentCoord.getY());
                Tile rookStartTile = board.getTile(0, y); // Rook at X=0
                Tile rookEndTile = board.getTile(currentCoord.getX() - 1, y); // Rook moves to X-1
                possibleMoves.add(new Move(board, currentTile, board.getTile(queenSideCastle), rookStartTile, rookEndTile));
            }
        }
        return possibleMoves;
    }

    // Helper method to check if castling is possible
    private boolean canCastle(Board board, Coordinate kingCoord, boolean kingSide) {
        int y = kingCoord.getY();
        int kingX = kingCoord.getX();
        int rookX = kingSide ? 7 : 0;
        int direction = kingSide ? 1 : -1;
        // Check rook
        Tile rookTile = board.getTile(rookX, y);
        if (!rookTile.hasPiece() || rookTile.getPiece().getType() != PieceTypes.ROOK || rookTile.getPiece().getTeam() != this.getTeam()) {
            return false;
        }
        chess_game.Pieces.Rook rook = (chess_game.Pieces.Rook) rookTile.getPiece();
        if (rook.hasMoved()) {
            return false;
        }
        // Check squares between king and rook are empty
        int start = Math.min(kingX, rookX) + 1;
        int end = Math.max(kingX, rookX) - 1;
        for (int x = start; x <= end; x++) {
            if (x == kingX) continue; // skip king's current square
            if (board.getTile(x, y).hasPiece()) {
                return false;
            }
        }
        // Check king does not pass through or end up in check
        for (int i = 1; i <= 2; i++) {
            Coordinate intermediate = new Coordinate(kingX + i * direction, y);
            Board testBoard = cloneBoardWithKingMove(board, kingCoord, intermediate);
            if (chess_game.Utilities.MoveUtilities.controlCheckState(testBoard, this.getTeam())) {
                return false;
            }
        }
        return true;
    }

    // Helper to clone the board and move the king for check simulation
    private Board cloneBoardWithKingMove(Board board, Coordinate from, Coordinate to) {
        // Create a proper deep copy of the board
        Board boardCopy = board.deepCopy();
        Tile fromTile = boardCopy.getTile(from);
        Tile toTile = boardCopy.getTile(to);
        Piece king = fromTile.getPiece();
        fromTile.setPiece(null);
        toTile.setPiece(king);
        return boardCopy;
    }

    @Override
    public Piece clone() {
        King copy = new King(this.getTeam());
        copy.setKilled(this.isKilled());
        copy.setHasMoved(this.hasMoved());
        copy.setCastlingDone(this.isCastlingDone());
        return copy;
    }
}