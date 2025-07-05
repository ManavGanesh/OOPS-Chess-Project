package server;

import Messages.ChatMessage;
import Messages.Message;
import Messages.PlayerInfo;
import Messages.PlayRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientListenThread extends Thread {

    SClient client;

    public ClientListenThread(SClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        while (!this.client.socket.isClosed()) {
            try {
                Message msg = (Message) (this.client.cInput.readObject());
                switch (msg.type) {
                    case PAIRING:
                        // Legacy quick-match pairing removed - only using request/response pairing
                        System.out.println("PAIRING message received but quick-match mode is disabled. Use PLAY_REQUEST/PLAY_RESPONSE instead.");
                        break;
                        
                    case PLAYER_LIST:
                        if (msg.content instanceof String) {
                            this.client.setPlayerName((String) msg.content);
                        }
                        this.client.isInPlayerSelection = true;
                        sendPlayerList();
                        break;
                        
                    case PLAY_REQUEST:
                        PlayRequest request = (PlayRequest) msg.content;
                        handlePlayRequest(request);
                        break;
                        
                    case PLAY_RESPONSE:
                        PlayRequest response = (PlayRequest) msg.content;
                        handlePlayResponse(response);
                        break;
                        
                    case CHAT:
                        if (this.client.isPaired && this.client.pair != null) {
                            ChatMessage chatMsg = (ChatMessage) msg.content;
                            Message forwardMsg = new Message(Message.MessageTypes.CHAT);
                            forwardMsg.content = chatMsg;
                            this.client.pair.Send(forwardMsg);
                        }
                        break;
                        
                    case MOVE:
                        if (this.client.isPaired && this.client.pair != null) {
                            // Send move to both players for proper synchronization
                            // First send to opponent
                            this.client.pair.Send(msg);
                            // Then send back to original player for confirmation
                            this.client.Send(msg);
                            System.out.println("DEBUG: Move sent to both players for synchronization");
                        }
                        break;
                        
                    case CHECK:
                        // Forward check message to paired client
                        if (this.client.pair != null) {
                            this.client.pair.Send(msg);
                        }
                        break;
                        
                    case CHECKMATE:
                    case STALEMATE:
                        // Forward checkmate/stalemate message to paired client
                        if (this.client.pair != null) {
                            this.client.pair.Send(msg);
                        }
                        break;
                        
                    case SAVE_GAME:
                        if (this.client.isPaired && this.client.pair != null) {
                            // Forward the save request to the paired client
                            this.client.pair.Send(msg);
                        }
                        // Store the game state on the server (could be saved to file/database)
                        System.out.println("Game saved: " + msg.content);
                        break;
                        
                    case LOAD_GAME:
                        // Handle load game request
                        if (this.client.isPaired && this.client.pair != null) {
                            this.client.pair.Send(msg);
                        }
                        break;
                        
                    case END:
                        // Forward end message to both players
                        if (this.client.pair != null) {
                            this.client.pair.Send(msg);
                            // Also send back to the original client to ensure they return to main menu
                            this.client.Send(msg);
                        }
                        break;

                    case TIMER_SYNC:
                        // Forward timer synchronization message to paired client
                        if (this.client.isPaired && this.client.pair != null) {
                            this.client.pair.Send(msg);
                            System.out.println("DEBUG: Timer sync message forwarded to paired client");
                        }
                        break;
                        
                    case TIMER_START:
                        // Forward timer start message to paired client
                        if (this.client.isPaired && this.client.pair != null) {
                            this.client.pair.Send(msg);
                            System.out.println("DEBUG: Timer start message forwarded to paired client");
                        }
                        break;
                        
                    case LEAVE:
                        this.client.isPaired = false;
                        this.client.isWantToPair = false;
                        this.client.isInPlayerSelection = false;
                        if (this.client.pair != null) {
                            Message leaveMsg = new Message(Message.MessageTypes.LEAVE);
                            this.client.pair.Send(leaveMsg);
                            this.client.pair.isWantToPair = false;
                            this.client.pair.isPaired = false;
                            this.client.pair.pair = null;
                        }
                        this.client.pair = null;
                        break;
                        
                    default:
                        System.out.println("Unknown message type received: " + msg.type);
                        break;
                }
            } catch (IOException ex) {
                Logger.getLogger(ClientListenThread.class.getName()).log(Level.SEVERE, null, ex);
                handleClientDisconnection();
                break;
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ClientListenThread.class.getName()).log(Level.SEVERE, null, ex);
                break;
            } catch (Exception ex) {
                System.out.println("Unexpected error in server ClientListenThread: " + ex.getMessage());
                ex.printStackTrace();
                break;
            }
        }
    }
    
    private void sendPlayerList() {
        ArrayList<PlayerInfo> availablePlayers = new ArrayList<>();
        synchronized (Server.clients) {
            for (SClient client : Server.clients) {
                if (client != this.client && !client.isPaired && 
                    (client.isInPlayerSelection || client.isWantToPair)) {
                    PlayerInfo playerInfo = new PlayerInfo(client.getPlayerName(), client.getClientId());
                    availablePlayers.add(playerInfo);
                }
            }
        }
        
        Message response = new Message(Message.MessageTypes.PLAYER_LIST);
        response.content = availablePlayers;
        this.client.Send(response);
    }
    
    private void handlePlayRequest(PlayRequest request) {
        // Find the target client
        SClient targetClient = null;
        synchronized (Server.clients) {
            for (SClient client : Server.clients) {
                if (client.getClientId().equals(request.toPlayerId)) {
                    targetClient = client;
                    break;
                }
            }
        }
        
        if (targetClient != null && !targetClient.isPaired) {
            // Forward the request to target client
            Message msg = new Message(Message.MessageTypes.PLAY_REQUEST);
            msg.content = request;
            targetClient.Send(msg);
        } else {
            // Send denial message
            Message denial = new Message(Message.MessageTypes.REQUEST_DENIED);
            denial.content = "Player is no longer available.";
            this.client.Send(denial);
        }
    }
    
    private void handlePlayResponse(PlayRequest response) {
        // Find the requester client
        SClient requesterClient = null;
        synchronized (Server.clients) {
            for (SClient client : Server.clients) {
                if (client.getClientId().equals(response.fromPlayerId)) {
                    requesterClient = client;
                    break;
                }
            }
        }
        
        if (requesterClient != null) {
            // Make final reference for use in lambda
            final SClient finalRequesterClient = requesterClient;
            
            if (response.isAccepted) {
                // Pair the clients
                this.client.pair = finalRequesterClient;
                finalRequesterClient.pair = this.client;
                this.client.isPaired = true;
                finalRequesterClient.isPaired = true;
                this.client.isInPlayerSelection = false;
                finalRequesterClient.isInPlayerSelection = false;
                
                // Send start messages with teams
                Message clientStartMessage = new Message(Message.MessageTypes.START);
                clientStartMessage.content = chess_game.Pieces.Team.WHITE;
                Message pairClientStartMessage = new Message(Message.MessageTypes.START);
                pairClientStartMessage.content = chess_game.Pieces.Team.BLACK;
                
                finalRequesterClient.Send(clientStartMessage);
                this.client.Send(pairClientStartMessage);
                
                System.out.println("Custom paired: " + finalRequesterClient.getPlayerName() + " (WHITE) vs " + this.client.getPlayerName() + " (BLACK)");
                
                // Send synchronized timer start after a delay to ensure both clients are ready
                new Thread(() -> {
                    try {
                        Thread.sleep(3000); // Give clients time to set up UI
                        Message timerStartMessage = new Message(Message.MessageTypes.TIMER_START);
                        timerStartMessage.content = System.currentTimeMillis(); // Send timestamp for perfect sync
                        
                        // Send to both clients simultaneously
                        finalRequesterClient.Send(timerStartMessage);
                        this.client.Send(timerStartMessage);
                        
                        System.out.println("DEBUG: Synchronized timer start sent to both clients");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }
            
            // Send response back to requester
            Message msg = new Message(Message.MessageTypes.PLAY_RESPONSE);
            msg.content = response;
            finalRequesterClient.Send(msg);
        }
    }
    
    private void handleClientDisconnection() {
        this.client.isPaired = false;
        this.client.isWantToPair = false;
        this.client.isInPlayerSelection = false;
        if (this.client.pair != null) {
            Message leaveMsg = new Message(Message.MessageTypes.LEAVE);
            this.client.pair.Send(leaveMsg);
            this.client.pair.isPaired = false;
            this.client.pair.pair = null;
        }
        this.client.pair = null;
        Server.clients.remove(this.client);
    }
}
