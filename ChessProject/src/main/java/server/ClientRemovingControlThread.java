/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import Messages.Message;
import java.util.Iterator;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */
// The purpose of this class is control the clients anytime.If any client
//lost the connection to the server but still in arraylist which keeps sclients in server,
//remove them from list.
public class ClientRemovingControlThread extends Thread {

    private Server server;

    public ClientRemovingControlThread(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        
        while(!this.server.socket.isClosed()) {
            try {
                synchronized (Server.clients) {
                    Iterator<SClient> iterator = Server.clients.iterator();
                    while (iterator.hasNext()) {
                        SClient client = iterator.next();
                        if (client.socket.isClosed()) {
                            // Notify paired client if exists
                            if (client.pair != null) {
                                Message leaveMsg = new Message(Message.MessageTypes.LEAVE);
                                client.pair.Send(leaveMsg);
                                client.pair.isPaired = false;
                                client.pair.pair = null;
                            }
                            System.out.println("Removing disconnected client: " + client.getPlayerName());
                            iterator.remove();
                        }
                    }
                }
                Thread.sleep(5000); // Check every 5 seconds
            } catch (InterruptedException ex) {
                System.out.println("ClientRemovingControlThread interrupted");
                break;
            }
        }
    }
}
