package ClientSide;

import Messages.Message;
import chess_game.Pieces.Team;
import chess_game.gui.Table;
import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Client {
    
    public Socket socket;
    public ObjectInputStream sInput;
    public ObjectOutputStream sOutput;
    public boolean isPaired;
    public Table game;
    public ClientListenThread clientListenThread;
    private Team team;
    private String clientId;
    private boolean movePending = false; // Track if a move is waiting for server response
    public Timer moveTimeoutTimer; // Timer to reset move pending state if no response
    
    public Client(Table game) {
        this.game = game;
        this.isPaired = false;
        this.clientId = UUID.randomUUID().toString(); // Generate unique ID
    }
    
    public void Connect(String serverIP, int serverPort) {
        try {
            System.out.println("DEBUG: Creating socket connection to " + serverIP + ":" + serverPort);
            this.socket = new Socket(serverIP, serverPort);
            System.out.println("DEBUG: Socket connected successfully");
            
            this.sOutput = new ObjectOutputStream(this.socket.getOutputStream());
            System.out.println("DEBUG: Output stream created");
            
            this.sInput = new ObjectInputStream(this.socket.getInputStream());
            System.out.println("DEBUG: Input stream created");
            
            this.clientListenThread = new ClientListenThread(this);
            this.clientListenThread.start();
            System.out.println("DEBUG: Client listen thread started. Client ID: " + this.clientId);
            
        } catch (IOException ex) {
            System.out.println("ERROR: Failed to connect to server: " + ex.getMessage());
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            this.socket = null;
        }
    }
    
    public void Send(Message message) {
        try {
            this.sOutput.writeObject(message);
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public Team getTeam() {
        return team;
    }
    
    public void setTeam(Team team) {
        this.team = team;
    }
    
    public String getClientId() {
        return this.clientId;
    }
    
    public boolean isMovePending() {
        return movePending;
    }
    
    public void setMovePending(boolean movePending) {
        this.movePending = movePending;
    }

    public void startMoveTimeout(int delay) {
        if (moveTimeoutTimer != null) {
            moveTimeoutTimer.cancel();
        }

        moveTimeoutTimer = new Timer();
        moveTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                setMovePending(false);
                System.out.println("DEBUG: Move pending state reset due to timeout.");
                
                // Update UI to reflect timeout on the UI thread
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (game != null && game.getBottomGameMenu() != null) {
                        if (game.getChessBoard() != null && game.getChessBoard().getCurrentPlayer().getTeam() == getTeam()) {
                            game.getBottomGameMenu().getTurnLBL().setText("Your Turn (Move Timeout)");
                            game.getBottomGameMenu().getTurnLBL().setForeground(Color.YELLOW);
                        } else {
                            game.getBottomGameMenu().getTurnLBL().setText("Enemy Turn");
                            game.getBottomGameMenu().getTurnLBL().setForeground(Color.RED);
                        }
                    }
                });
            }
        }, delay);
    }
}
