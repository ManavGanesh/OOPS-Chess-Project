/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess_game.Pieces;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */

//This enum is using for surely define the type of pieces.
public enum PieceTypes
{
    QUEEN(9),
    KING(100),
    ROOK(5),
    BISHOP(3),
    KNIGHT(3),
    PAWN(1),
    EMPTY(0);

    private final int points;

    PieceTypes(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }
}