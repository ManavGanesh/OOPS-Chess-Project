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

//This class created for optimized chat message transmission. 
//Contains player name, message content and timestamp for chat functionality.
public class ChatMessage implements java.io.Serializable {
    public String playerName;
    public String message;
    public long timestamp;
    
    public ChatMessage(String playerName, String message) {
        this.playerName = playerName;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}
