package server;

import chess_game.Pieces.Team;
import java.io.Serializable;

/**
 * Class to hold start information for a chess game including team assignment and player names
 */
public class StartInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Team team;
    private String playerName;
    private String opponentName;
    
    public StartInfo(Team team, String playerName, String opponentName) {
        this.team = team;
        this.playerName = playerName;
        this.opponentName = opponentName;
    }
    
    public Team getTeam() {
        return team;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public String getOpponentName() {
        return opponentName;
    }
    
    @Override
    public String toString() {
        return "StartInfo{" +
               "team=" + team +
               ", playerName='" + playerName + '\'' +
               ", opponentName='" + opponentName + '\'' +
               '}';
    }
}
