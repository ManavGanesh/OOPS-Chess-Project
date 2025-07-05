/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Messages;

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
    
    public PlayRequest(String fromPlayerName, String fromPlayerId, String toPlayerName, String toPlayerId) {
        this.fromPlayerName = fromPlayerName;
        this.fromPlayerId = fromPlayerId;
        this.toPlayerName = toPlayerName;
        this.toPlayerId = toPlayerId;
        this.isAccepted = false;
        this.isRejected = false;
    }
}
