/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */

// This is the main class of server to start the server.
public class Start {

    public static void main(String[] args) {
        System.out.println("Starting Chess Server on port 4000...");
        Server server = new Server(4000);
        
        // Check if server initialization was successful
        if (server.socket == null) {
            System.out.println("Server initialization failed. Exiting...");
            System.exit(1);
        }
        
        server.ListenClientConnectionRequests();

        while (!server.socket.isClosed()) {
            try {
                int clientCount = Server.clients.size();
                int pairedCount = 0;
                int waitingCount = 0;
                
                synchronized (Server.clients) {
                    for (SClient client : Server.clients) {
                        if (client.isPaired) {
                            pairedCount++;
                        } else if (client.isWantToPair) {
                            waitingCount++;
                        }
                    }
                }
                
                System.out.println("Server Status - Total clients: " + clientCount + 
                                 ", Paired: " + pairedCount + 
                                 ", Waiting: " + waitingCount);
                Thread.sleep(5000); // Update every 5 seconds
            } catch (InterruptedException ex) {
                Logger.getLogger(Start.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
