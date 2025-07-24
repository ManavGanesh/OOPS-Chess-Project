/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import Messages.Message;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */

//This is a TCP protocol connection based server.
public class Server {

    public ServerSocket socket;
    public int port;
    public ListenConnectionRequestThread listenConnectionRequestThread;
    public ClientRemovingControlThread removingControlThread;
    public static ArrayList<SClient> clients;

    //lock mechanism for pairing thread. One client can match with one client at the same time. So we use the lock mechanism to provide
    //other clients not try to pair this client at the same time.
    public static Semaphore pairingLockForTwoPair = new Semaphore(1, true);

    public Server(int port) {
        try {
            this.port = port;
            this.socket = new ServerSocket(this.port);
            this.listenConnectionRequestThread = new ListenConnectionRequestThread(this);
            removingControlThread = new ClientRemovingControlThread(this);
            this.clients = new ArrayList<SClient>();
            System.out.println("Server initialized successfully on port: " + this.port);
            
        } catch (IOException ex) {
            System.out.println("There is an error occurred when opening the server on port: " + this.port);
            System.out.println("Error details: " + ex.getMessage());
            System.out.println("Please check if port " + this.port + " is already in use.");
            
            // Set socket to null to indicate failure
            this.socket = null;
            this.listenConnectionRequestThread = null;
            this.removingControlThread = null;
            this.clients = null;
        }
    }

    // starts the acceptance
    public void ListenClientConnectionRequests() {
        if (this.socket == null || this.listenConnectionRequestThread == null) {
            System.out.println("Cannot start server threads. Server initialization failed.");
            return;
        }
        
        this.listenConnectionRequestThread.start();
        this.removingControlThread.start(); // Start the removing control thread
        System.out.println("Server threads started successfully.");
    }

    public static void SendMessage(SClient client, Message message) {
        try {
            if (client != null && !client.socket.isClosed()) {
                client.cOutput.writeObject(message);
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            // Remove client from list if send fails
            clients.remove(client);
        }
    }

    public static void SendMessage(SClient client, String message) {
        try {
            if (client != null && !client.socket.isClosed()) {
                client.cOutput.writeObject(message);
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            // Remove client from list if send fails
            clients.remove(client);
        }
    }
    
    public static void SendMessage(SClient client, Object object) {
        try {
            if (client != null && !client.socket.isClosed()) {
                client.cOutput.writeObject(object);
            }
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            // Remove client from list if send fails
            clients.remove(client);
        }
    }
}
