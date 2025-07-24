/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Messages;

import chess_game.Pieces.Team;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */

//This class handles play requests between players
public class PlayRequest implements java.io.Serializable {
    public String fromPlayerName;
    public String fromPlayerId;
    public String toPlayerName;
    public String toPlayerId;
    public boolean isAccepted;
    public boolean isRejected;
    
    // Load game functionality
    public boolean isLoadGameRequest = false;
    public String loadGameName = null;
    public GameState loadGameState = null;
    
    // Color selection functionality
    public Team requesterPreferredColor = null;
    
    // FIX: Store opponent names for both players to ensure correct display
    public String requesterOpponentName = null; // Who the requester sees as opponent
    public String responderOpponentName = null; // Who the responder sees as opponent
    
    public PlayRequest(String fromPlayerName, String fromPlayerId, String toPlayerName, String toPlayerId) {
        this.fromPlayerName = fromPlayerName;
        this.fromPlayerId = fromPlayerId;
        this.toPlayerName = toPlayerName;
        this.toPlayerId = toPlayerId;
        this.isAccepted = false;
        this.isRejected = false;
    }
    
    // Load game methods
    public void setLoadGameRequest(boolean isLoadGameRequest) {
        this.isLoadGameRequest = isLoadGameRequest;
    }
    
    public boolean isLoadGameRequest() {
        return this.isLoadGameRequest;
    }
    
    public void setLoadGameName(String loadGameName) {
        this.loadGameName = loadGameName;
    }
    
    public String getLoadGameName() {
        return this.loadGameName;
    }
    
    public void setLoadGameState(GameState loadGameState) {
        this.loadGameState = loadGameState;
    }
    
    public GameState getLoadGameState() {
        return this.loadGameState;
    }
    
    // Color selection methods
    public void setRequesterPreferredColor(Team color) {
        this.requesterPreferredColor = color;
    }
    
    public Team getRequesterPreferredColor() {
        return this.requesterPreferredColor;
    }
    
    // Opponent name methods
    public void setRequesterOpponentName(String requesterOpponentName) {
        this.requesterOpponentName = requesterOpponentName;
    }
    
    public String getRequesterOpponentName() {
        return this.requesterOpponentName;
    }
    
    public void setResponderOpponentName(String responderOpponentName) {
        this.responderOpponentName = responderOpponentName;
    }
    
    public String getResponderOpponentName() {
        return this.responderOpponentName;
    }
}
