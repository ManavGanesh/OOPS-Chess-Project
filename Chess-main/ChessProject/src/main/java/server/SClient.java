package server;

import Messages.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SClient {

    public Socket socket;
    public ObjectInputStream cInput;
    public ObjectOutputStream cOutput;
    public ClientListenThread clientListenThread;
    public SClient pair;
    public boolean isPaired;
    public boolean isWantToPair = false;
    public boolean isInPlayerSelection = false;
    public ClientPairingThread pairingThread;
    public String playerName;
    public String clientId;
    
    public SClient(Socket socket) {
        try {
            this.socket = socket;
            this.cOutput = new ObjectOutputStream(this.socket.getOutputStream());
            this.cInput = new ObjectInputStream(this.socket.getInputStream());
            this.clientListenThread = new ClientListenThread(this);
            this.pairingThread = new ClientPairingThread(this);
            this.isPaired = false;
            this.playerName = "Anonymous";
            this.clientId = UUID.randomUUID().toString();
        } catch (IOException ex) {
            Logger.getLogger(SClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void Send(Object msg) {
        try {
            this.cOutput.writeObject(msg);
        } catch (IOException ex) {
            Logger.getLogger(SClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void Listen() {
        this.clientListenThread.start();
    }
    
    public void setPlayerName(String playerName) {
        this.playerName = playerName != null ? playerName : "Anonymous";
    }
    
    public String getPlayerName() {
        return this.playerName;
    }
    
    public String getClientId() {
        return this.clientId;
    }
}
