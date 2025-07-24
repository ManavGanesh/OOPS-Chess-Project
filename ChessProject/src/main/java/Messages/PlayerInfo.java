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

//This class contains player information for displaying in player selection
public class PlayerInfo implements java.io.Serializable {
    public String playerName;
    public String playerId; // Unique identifier
    public boolean isAvailable;
    
    public PlayerInfo(String playerName, String playerId) {
        this.playerName = playerName;
        this.playerId = playerId;
        this.isAvailable = true;
    }
}
