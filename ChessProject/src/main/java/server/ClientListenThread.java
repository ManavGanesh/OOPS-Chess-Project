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
                        // Only set player name if it's not already set (from username registration)
                        if (msg.content instanceof String && this.client.getPlayerName() == null) {
                            this.client.setPlayerName((String) msg.content);
                        }
                        this.client.isInPlayerSelection = true;
                        System.out.println("DEBUG: Player list requested by " + this.client.getPlayerName());
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
                        if (msg.content instanceof Messages.GameState) {
                            Messages.GameState gameState = (Messages.GameState) msg.content;
                            System.out.println("Load game request from " + this.client.playerName + 
                                             " for game: " + gameState.getSaveName());
                            
                            // Store the game state with the client for later pairing
                            this.client.setLoadedGameState(gameState);
                            this.client.setWantToLoadGame(true);
                            
                            // If client is paired, forward to opponent
                            if (this.client.isPaired && this.client.pair != null) {
                                this.client.pair.Send(msg);
                                System.out.println("Load game request forwarded to paired client: " + 
                                                 this.client.pair.playerName);
                            } else {
                                // Client is not paired, try to find an opponent
                                System.out.println("Client not paired, attempting to find opponent for load game");
                                // The pairing logic will handle this in the pairing thread
                            }
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
                        
                    case USERNAME_REGISTER:
                        Messages.UsernameRegisterMessage registerRequest = (Messages.UsernameRegisterMessage) msg.content;
                        handleUsernameRegistration(registerRequest);
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
                        
                        // Clean up username from registry when leaving
                        if (this.client.getPlayerName() != null) {
                            UserRegistry.unregisterUsername(this.client.getPlayerName());
                            System.out.println("DEBUG: Cleaned up username for client leaving: " + this.client.getPlayerName());
                        }
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
        
        System.out.println("DEBUG: sendPlayerList called by " + this.client.getPlayerName());
        System.out.println("DEBUG: Total clients connected: " + Server.clients.size());
        
        synchronized (Server.clients) {
            for (SClient client : Server.clients) {
                System.out.println("DEBUG: Checking client: " + client.getPlayerName() + 
                                 ", isPaired: " + client.isPaired + 
                                 ", isInPlayerSelection: " + client.isInPlayerSelection + 
                                 ", isWantToPair: " + client.isWantToPair);
                                 
                if (client != this.client && !client.isPaired && 
                    (client.isInPlayerSelection || client.isWantToPair)) {
                    PlayerInfo playerInfo = new PlayerInfo(client.getPlayerName(), client.getClientId());
                    availablePlayers.add(playerInfo);
                    System.out.println("DEBUG: Added player to list: " + client.getPlayerName());
                }
            }
        }
        
        System.out.println("DEBUG: Player list requested by " + this.client.getPlayerName() + ", sending " + availablePlayers.size() + " players");
        for (PlayerInfo player : availablePlayers) {
            System.out.println("DEBUG: Available player: " + player.playerName + " (ID: " + player.playerId + ")");
        }
        
        try {
            Message response = new Message(Message.MessageTypes.PLAYER_LIST);
            response.content = availablePlayers;
            this.client.Send(response);
            System.out.println("DEBUG: Player list response sent successfully to " + this.client.getPlayerName());
        } catch (Exception e) {
            System.err.println("ERROR: Failed to send player list response: " + e.getMessage());
            e.printStackTrace();
        }
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
                
                // Determine team assignment based on requester's color preference or load game state
                chess_game.Pieces.Team requesterTeam;
                chess_game.Pieces.Team responderTeam;
                
                if (response.isLoadGameRequest() && response.getLoadGameState() != null) {
                    // Load game scenario - preserve original player colors
                    Messages.GameState loadGameState = response.getLoadGameState();
                    if (loadGameState.getPlayer1Color() != null && loadGameState.getPlayer2Color() != null) {
                        // Determine which player is which based on names
                        if (finalRequesterClient.getPlayerName().equals(loadGameState.getPlayer1Name())) {
                            requesterTeam = loadGameState.getPlayer1Color();
                            responderTeam = loadGameState.getPlayer2Color();
                        } else if (finalRequesterClient.getPlayerName().equals(loadGameState.getPlayer2Name())) {
                            requesterTeam = loadGameState.getPlayer2Color();
                            responderTeam = loadGameState.getPlayer1Color();
                        } else {
                            // Fallback to color preference if names don't match
                            if (response.getRequesterPreferredColor() != null) {
                                requesterTeam = response.getRequesterPreferredColor();
                                responderTeam = (requesterTeam == chess_game.Pieces.Team.WHITE) ? 
                                               chess_game.Pieces.Team.BLACK : chess_game.Pieces.Team.WHITE;
                            } else {
                                requesterTeam = chess_game.Pieces.Team.WHITE;
                                responderTeam = chess_game.Pieces.Team.BLACK;
                            }
                        }
                    } else {
                        // Fallback for old saves without color information
                        if (response.getRequesterPreferredColor() != null) {
                            requesterTeam = response.getRequesterPreferredColor();
                            responderTeam = (requesterTeam == chess_game.Pieces.Team.WHITE) ? 
                                           chess_game.Pieces.Team.BLACK : chess_game.Pieces.Team.WHITE;
                        } else {
                            requesterTeam = chess_game.Pieces.Team.WHITE;
                            responderTeam = chess_game.Pieces.Team.BLACK;
                        }
                    }
                } else if (response.getRequesterPreferredColor() != null) {
                    // Normal game with color preference
                    requesterTeam = response.getRequesterPreferredColor();
                    responderTeam = (requesterTeam == chess_game.Pieces.Team.WHITE) ? 
                                   chess_game.Pieces.Team.BLACK : chess_game.Pieces.Team.WHITE;
                } else {
                    // Random assignment - requester gets white, responder gets black
                    requesterTeam = chess_game.Pieces.Team.WHITE;
                    responderTeam = chess_game.Pieces.Team.BLACK;
                }
                
                // Send start messages with assigned teams
                // CORRECTED: Use proper name assignment - player gets their own name as playerName
                Message clientStartMessage = new Message(Message.MessageTypes.START);
                clientStartMessage.content = new StartInfo(requesterTeam, response.fromPlayerName, response.toPlayerName);
                Message pairClientStartMessage = new Message(Message.MessageTypes.START);
                pairClientStartMessage.content = new StartInfo(responderTeam, response.toPlayerName, response.fromPlayerName);
                
                finalRequesterClient.Send(clientStartMessage);
                this.client.Send(pairClientStartMessage);
                
                System.out.println("Custom paired: " + finalRequesterClient.getPlayerName() + " (" + requesterTeam + ") vs " + this.client.getPlayerName() + " (" + responderTeam + ")");
                
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
        
        // Clean up username from registry
        if (this.client.getPlayerName() != null) {
            UserRegistry.unregisterUsername(this.client.getPlayerName());
            System.out.println("DEBUG: Cleaned up username for disconnected client: " + this.client.getPlayerName());
        }
        
        Server.clients.remove(this.client);
    }
    
    private void handleUsernameRegistration(Messages.UsernameRegisterMessage request) {
        String requestedUsername = request.getRequestedUsername();
        System.out.println("DEBUG: Username registration request for: " + requestedUsername);
        
        // Try to register the username
        String registeredUsername = UserRegistry.registerUsername(requestedUsername);
        
        Messages.UsernameRegisterMessage response;
        if (registeredUsername != null) {
            // Registration successful
            this.client.setPlayerName(registeredUsername);
            response = new Messages.UsernameRegisterMessage(
                requestedUsername, 
                registeredUsername, 
                true, 
                null
            );
            System.out.println("DEBUG: Username registration successful: " + registeredUsername);
        } else {
            // Registration failed - username already taken
            response = new Messages.UsernameRegisterMessage(
                requestedUsername, 
                null, 
                false, 
                "Username '" + requestedUsername + "' is already taken."
            );
            System.out.println("DEBUG: Username registration failed: " + requestedUsername + " is already taken");
        }
        
        // Send response back to client
        Message responseMsg = new Message(Message.MessageTypes.USERNAME_REGISTER_RESPONSE);
        responseMsg.content = response;
        this.client.Send(responseMsg);
    }
}
