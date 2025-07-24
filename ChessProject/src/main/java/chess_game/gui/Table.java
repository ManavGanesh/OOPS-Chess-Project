package chess_game.gui;

import ClientSide.Client;
import Messages.ChatMessage;
import Messages.GameState;
import Messages.Message;
import Messages.PlayerInfo;
import Messages.PlayRequest;
import chess_game.Boards.Board;
import chess_game.Pieces.Team;
import chess_game.Resources.GUI_Configurations;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import chess_game.ChessAI;
import chess_game.Move.Move;
import chess_game.Utilities.GameLogic;
import javax.swing.SwingWorker;

public class Table {

    private JFrame gameFrame;
    private BoardPanel boardPanel;
    private Board chessBoard;
    private MainMenu mainMenu;
    private PlayerSelectionPanel playerSelectionPanel;
    private InGameBottomMenu bottomGameMenu;
    private Client client;
    private String playerName;
    private String registeredPlayerName; // Locked username for the session
    private String opponentName;
    public Team aiTeam;
    public Team humanTeam;
    private boolean aiMoveInProgress = false;
    private Move lastMove = null; // Track the last move made
    private boolean usernameRegistered = false; // Track if username is registered for this session
    private boolean usernameLockedForSession = false; // Track if username is locked for this instance

    public Table() {
        this.gameFrame = new JFrame("Chess");
        this.gameFrame.setLayout(new BorderLayout());
        this.gameFrame.setSize(GUI_Configurations.OUTER_FRAME_DIMENSION);
        this.gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.mainMenu = new MainMenu();
        this.playerSelectionPanel = new PlayerSelectionPanel();
        this.client = new Client(this);
        
        // Try to connect to server, but don't exit if connection fails (for AI mode)
        try {
            String ipadd = "192.168.29.129"; // Use localhost for local testing
            System.out.println("DEBUG: Attempting to connect to server at "+ipadd+":4000");
            this.client.Connect( ipadd , 4000);
            if (this.client.socket == null) {
                System.out.println("Warning: Could not connect to server. Online features will be disabled.");
                // Don't exit - allow AI mode to work
            } else {
                System.out.println("DEBUG: Successfully connected to server!");
            }
        } catch (Exception e) {
            System.out.println("Warning: Server connection failed. Online features will be disabled. " + e.getMessage());
            // Don't exit - allow AI mode to work
        }
        
        // Show initial username registration dialog
        showUsernameRegistrationDialog();
        
        createMainMenu();
        this.gameFrame.setVisible(true);
    }
    
    private void showUsernameRegistrationDialog() {
        // Keep asking for username until a valid one is provided
        while (!usernameLockedForSession) {
            String inputName = JOptionPane.showInputDialog(
                gameFrame,
                "Enter your username:",
                "Username",
                JOptionPane.QUESTION_MESSAGE
            );
            
            // If user cancels, exit the application
            if (inputName == null) {
                System.exit(0);
                return;
            }
            
            // Validate username
            inputName = inputName.trim();
            if (inputName.isEmpty()) {
                JOptionPane.showMessageDialog(
                    gameFrame,
                    "Username cannot be empty.",
                    "Invalid Username",
                    JOptionPane.ERROR_MESSAGE
                );
                continue;
            }
            
            // Try to register the username with the server
            if (registerUsernameWithServer(inputName)) {
                // Successfully registered - lock the username for this session
                this.playerName = registeredPlayerName;
                this.usernameRegistered = true;
                this.usernameLockedForSession = true;
                
                // Update the main menu text field to show the locked username and make it appear disabled
                this.mainMenu.getPlayerNameTXT().setText(registeredPlayerName);
                this.mainMenu.getPlayerNameTXT().setEditable(false); // Make it read-only
                this.mainMenu.getPlayerNameTXT().setEnabled(false); // Make it appear disabled
                this.mainMenu.getPlayerNameTXT().setToolTipText("Username is locked for this session");
                
                System.out.println("DEBUG: Username locked for session: " + registeredPlayerName);
                
                break;
            }
            // If registration failed, the error message was already shown in registerUsernameWithServer
        }
        
        // Add shutdown hook to unregister username when application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (registeredPlayerName != null) {
                // Send unregister message to server
                try {
                    if (client != null && client.socket != null && !client.socket.isClosed()) {
                        Message unregisterMsg = new Message(Message.MessageTypes.LEAVE);
                        client.Send(unregisterMsg);
                    }
                } catch (Exception e) {
                    System.out.println("DEBUG: Could not send unregister message to server on exit");
                }
                System.out.println("DEBUG: Unregistered username on application exit: " + registeredPlayerName);
            }
        }));
    }
    
    private boolean registerUsernameWithServer(String username) {
        // Check if client is connected to server
        if (client == null || client.socket == null || client.socket.isClosed()) {
            JOptionPane.showMessageDialog(
                gameFrame,
                "Not connected to server. Cannot register username.",
                "Connection Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
        
        try {
            // Send username registration request to server
            Messages.UsernameRegisterMessage registerRequest = new Messages.UsernameRegisterMessage(username);
            Message msg = new Message(Message.MessageTypes.USERNAME_REGISTER);
            msg.content = registerRequest;
            client.Send(msg);
            
            System.out.println("DEBUG: Sent username registration request for: " + username);
            
            // Wait for server response (with timeout)
            synchronized (this) {
                try {
                    this.wait(5000); // Wait up to 5 seconds for response
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            // Check if registration was successful
            if (registeredPlayerName != null) {
                System.out.println("DEBUG: Username registration successful: " + registeredPlayerName);
                return true;
            } else {
                System.out.println("DEBUG: Username registration failed or timed out");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to register username with server: " + e.getMessage());
            JOptionPane.showMessageDialog(
                gameFrame,
                "Failed to register username. Please try again.",
                "Registration Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }
    
    public void handleUsernameRegistrationResponse(Messages.UsernameRegisterMessage response) {
        System.out.println("DEBUG: Received username registration response: " + response.isSuccess());
        
        if (response.isSuccess()) {
            this.registeredPlayerName = response.getRegisteredUsername();
            System.out.println("DEBUG: Username registered successfully: " + registeredPlayerName);
        } else {
            this.registeredPlayerName = null;
            String errorMessage = response.getErrorMessage();
            if (errorMessage == null) {
                errorMessage = "Username '" + response.getRequestedUsername() + "' is already taken.";
            }
            JOptionPane.showMessageDialog(
                gameFrame,
                errorMessage,
                "Username Registration Failed",
                JOptionPane.WARNING_MESSAGE
            );
            System.out.println("DEBUG: Username registration failed: " + errorMessage);
        }
        
        // Notify waiting thread
        synchronized (this) {
            this.notify();
        }
    }
    
    public void createMainMenu() {
        this.gameFrame.getContentPane().removeAll();
        this.mainMenu.getInfoLBL().setText("");
        this.mainMenu.getInfoLBL().setVisible(false);
        
        // Reset client state when returning to main menu
        if (this.client != null) {
            this.client.isPaired = false;
            this.client.setTeam(null);
            this.client.setMovePending(false);
            this.client.setWantToLoadGame(false);
            this.client.setLoadedGameState(null);
            if (this.client.moveTimeoutTimer != null) {
                this.client.moveTimeoutTimer.cancel();
                this.client.moveTimeoutTimer = null;
            }
            
            // DON'T unregister username when returning to main menu
            // Username should remain locked for the entire session
            System.out.println("DEBUG: Returning to main menu, username remains locked: " + this.playerName);
        }
        
        // Reset game state references
        this.chessBoard = null;
        this.boardPanel = null;
        this.bottomGameMenu = null;
        this.opponentName = null;
        
        // Remove all existing action listeners to prevent duplicates
        for (ActionListener al : this.mainMenu.getPlayBTN().getActionListeners()) {
            this.mainMenu.getPlayBTN().removeActionListener(al);
        }
        
        // Quick match button handler - now goes directly to player selection
        this.mainMenu.getPlayBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Use the locked username instead of reading from text field
                if (!usernameLockedForSession || registeredPlayerName == null) {
                    JOptionPane.showMessageDialog(gameFrame, "Username registration error. Please restart the application.");
                    return;
                }
                playerName = registeredPlayerName;
                System.out.println("DEBUG: Quick Match clicked, using locked username: " + playerName);
                createPlayerSelectionPanel();
            }
        });
        
        // Remove all existing action listeners from Load Game button to prevent duplicates
        for (ActionListener al : this.mainMenu.getLoadGameBTN().getActionListeners()) {
            this.mainMenu.getLoadGameBTN().removeActionListener(al);
        }
        
        this.mainMenu.getLoadGameBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Use the locked username instead of reading from text field
                if (!usernameLockedForSession || registeredPlayerName == null) {
                    JOptionPane.showMessageDialog(gameFrame, "Username registration error. Please restart the application.");
                    return;
                }
                playerName = registeredPlayerName;
                showLoadGameDialog();
            }
        });
        
        // Add AI Play button handler
        for (ActionListener al : this.mainMenu.getAIPlayBTN().getActionListeners()) {
            this.mainMenu.getAIPlayBTN().removeActionListener(al);
        }
        this.mainMenu.getAIPlayBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Use the locked username instead of reading from text field
                if (!usernameLockedForSession || registeredPlayerName == null) {
                    JOptionPane.showMessageDialog(gameFrame, "Username registration error. Please restart the application.");
                    return;
                }
                playerName = registeredPlayerName;
                System.out.println("DEBUG: AI Play clicked, using locked username: " + playerName);
                
                // Show color selection dialog
                ColorSelectionDialog colorDialog = new ColorSelectionDialog(gameFrame);
                colorDialog.setVisible(true);
                
                if (colorDialog.isColorSelected()) {
                    Team selectedTeam = colorDialog.getSelectedTeam();
                    createAIGamePanel(selectedTeam);  // Create game with the selected color
                }
            }
        });
        
        // Add Exit button handler
        for (ActionListener al : this.mainMenu.getExitBTN().getActionListeners()) {
            this.mainMenu.getExitBTN().removeActionListener(al);
        }
        this.mainMenu.getExitBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = JOptionPane.showConfirmDialog(
                    gameFrame,
                    "Are you sure you want to exit the game?",
                    "Exit Game",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (response == JOptionPane.YES_OPTION) {
                    // Properly unregister username before exit
                    if (registeredPlayerName != null) {
                        server.UserRegistry.unregisterUsername(registeredPlayerName);
                        System.out.println("DEBUG: Unregistered username on exit: " + registeredPlayerName);
                    }
                    System.exit(0);
                }
            }
        });
        
        this.gameFrame.add(mainMenu, BorderLayout.CENTER);
        this.gameFrame.revalidate();
        this.gameFrame.repaint();
    }

    public void createPlayerSelectionPanel() {
        this.gameFrame.getContentPane().removeAll();
        
        // Setup event handlers
        setupPlayerSelectionHandlers();
        
        // Use the already registered and locked username
        if (!usernameLockedForSession || registeredPlayerName == null) {
            JOptionPane.showMessageDialog(gameFrame, 
                "Username registration error. Please restart the application.", 
                "Registration Error", 
                JOptionPane.ERROR_MESSAGE);
            createMainMenu();
            return;
        }
        
        String registeredName = registeredPlayerName;
        this.playerName = registeredName;
        System.out.println("DEBUG: Using locked username in player selection: " + registeredName);
        
        // Check if client is connected to server
        if (client == null || client.socket == null || client.socket.isClosed()) {
            JOptionPane.showMessageDialog(gameFrame, 
                "Not connected to server. Multiplayer features are not available.", 
                "Connection Error", 
                JOptionPane.ERROR_MESSAGE);
            createMainMenu();
            return;
        }
        
        // Send player list request
        try {
            Message msg = new Message(Message.MessageTypes.PLAYER_LIST);
            msg.content = registeredName;
            client.Send(msg);
            System.out.println("DEBUG: Player list request sent for: " + registeredName);
            
            // Update status to show we're loading
            playerSelectionPanel.getStatusLBL().setText("Loading players...");
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to send player list request: " + e.getMessage());
            JOptionPane.showMessageDialog(gameFrame, 
                "Failed to request player list. Please try again.", 
                "Network Error", 
                JOptionPane.ERROR_MESSAGE);
        }
        
        this.gameFrame.add(playerSelectionPanel, BorderLayout.CENTER);
        this.gameFrame.revalidate();
        this.gameFrame.repaint();
        
        // Add a small delay to ensure UI is fully initialized, then request player list
        javax.swing.Timer delayedRefresh = new javax.swing.Timer(500, e -> {
            // Send another player list request after UI is set up
            try {
                System.out.println("DEBUG: Sending delayed player list request");
                Message refreshMsg = new Message(Message.MessageTypes.PLAYER_LIST);
                refreshMsg.content = registeredName;
                client.Send(refreshMsg);
            } catch (Exception ex) {
                System.err.println("DEBUG: Delayed refresh failed: " + ex.getMessage());
            }
            ((javax.swing.Timer) e.getSource()).stop();
        });
        delayedRefresh.setRepeats(false);
        delayedRefresh.start();
    }

    private void setupPlayerSelectionHandlers() {
        // Remove existing listeners
        for (ActionListener al : this.playerSelectionPanel.getSendRequestBTN().getActionListeners()) {
            this.playerSelectionPanel.getSendRequestBTN().removeActionListener(al);
        }
        for (ActionListener al : this.playerSelectionPanel.getRefreshBTN().getActionListeners()) {
            this.playerSelectionPanel.getRefreshBTN().removeActionListener(al);
        }
        for (ActionListener al : this.playerSelectionPanel.getBackBTN().getActionListeners()) {
            this.playerSelectionPanel.getBackBTN().removeActionListener(al);
        }
        
        // Send request button
        this.playerSelectionPanel.getSendRequestBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PlayerInfo selectedPlayer = playerSelectionPanel.getSelectedPlayer();
                if (selectedPlayer != null) {
                    System.out.println("DEBUG: Requesting game with " + selectedPlayer.playerName);
                    
                    // Check if this is a load game request
                    boolean isLoadGameRequest = client.isWantToLoadGame() && client.getLoadedGameState() != null;
                    String gameType = isLoadGameRequest ? "load game" : "new game";
                    String gameName = isLoadGameRequest ? client.getLoadedGameState().getSaveName() : "";
                    
                    System.out.println("DEBUG: This is a " + gameType + " request" + 
                                     (isLoadGameRequest ? " for game: " + gameName : ""));
                    
                    // If we're already paired, we need to leave the current game first
                    if (client.isPaired) {
                        System.out.println("DEBUG: Player is already paired, leaving current game to send new request");
                        // Send leave message to server
                        Message leaveMsg = new Message(Message.MessageTypes.LEAVE);
                        client.Send(leaveMsg);
                        // Reset client state
                        client.isPaired = false;
                    }

                    // Store opponent name but DON'T create game panel yet
                    // Wait for server confirmation via START message
                    setOpponentName(selectedPlayer.playerName);
                    
                    // DON'T set team or paired state here - let server handle it
                    // Remove these problematic lines:
                    // client.isPaired = true;
                    // client.setTeam(Team.WHITE);
                    // client.setMovePending(false);

                    // DON'T create game panel immediately - wait for START message
                    // createGamePanel();

                    PlayRequest request = new PlayRequest(playerName, client.getClientId(), 
                                                        selectedPlayer.playerName, selectedPlayer.playerId);
                    
                    // FIX: Set opponent names for both players to ensure correct display
                    request.setRequesterOpponentName(selectedPlayer.playerName); // Requester sees selected player as opponent
                    request.setResponderOpponentName(playerName); // Responder sees requester as opponent
                    
                    // Add color selection to the request
                    Team selectedColor = playerSelectionPanel.getSelectedColor();
                    if ("Random".equals(playerSelectionPanel.colorSelectionCombo.getSelectedItem())) {
                        // For random, we'll let the server decide
                        selectedColor = null;
                    }
                    request.setRequesterPreferredColor(selectedColor);
                    
                    // Add load game information to the request
                    if (isLoadGameRequest) {
                        request.setLoadGameRequest(true);
                        request.setLoadGameName(gameName);
                        request.setLoadGameState(client.getLoadedGameState());
                    }
                    
                    Message msg = new Message(Message.MessageTypes.PLAY_REQUEST);
                    msg.content = request;
                    client.Send(msg);

                    // Update status message based on request type
                    String statusMessage = isLoadGameRequest ? 
                        "Load game request sent to " + selectedPlayer.playerName + " for game: " + gameName + " - waiting for response..." :
                        "Play request sent to " + selectedPlayer.playerName + " - waiting for response...";
                    
                    playerSelectionPanel.getStatusLBL().setText(statusMessage);
                    playerSelectionPanel.getSendRequestBTN().setEnabled(false);
                    
                    // Add timeout for requester - if no response in 5 seconds, enable retry
                    javax.swing.Timer requesterTimeoutTimer = new javax.swing.Timer(5000, event -> {
                        System.out.println("DEBUG: Request timeout - no response received from " + selectedPlayer.playerName);
                        
                        // Re-enable the send request button and update status
                        if (playerSelectionPanel != null) {
                            playerSelectionPanel.getSendRequestBTN().setEnabled(true);
                            String timeoutMessage = isLoadGameRequest ?
                                "Load game request timed out. Try again or select another player." :
                                "Request timed out. Try again or select another player.";
                            playerSelectionPanel.getStatusLBL().setText(timeoutMessage);
                        }
                        
                        // Popup removed - keeping all other original logic
                        // Send a fallback message to server to establish pairing
                        try {
                            System.out.println("DEBUG: Sending fallback pairing message to server");
                            Message fallbackMsg = new Message(Message.MessageTypes.PLAY_RESPONSE);
                            PlayRequest fallbackRequest = new PlayRequest(
                                selectedPlayer.playerName,  // Fake response from target player
                                selectedPlayer.playerId,     // Target player ID
                                playerName,                  // To requester
                                client.getClientId()         // Requester ID
                            );
                            fallbackRequest.isAccepted = true;
                            
                            // Include load game information in fallback request
                            if (isLoadGameRequest) {
                                fallbackRequest.setLoadGameRequest(true);
                                fallbackRequest.setLoadGameName(gameName);
                                fallbackRequest.setLoadGameState(client.getLoadedGameState());
                            }
                            
                            fallbackMsg.content = fallbackRequest;
                            client.Send(fallbackMsg);
                            System.out.println("DEBUG: Fallback pairing message sent");
                        } catch (Exception msgEx) {
                            System.err.println("DEBUG: Failed to send fallback message: " + msgEx.getMessage());
                        }
                        
                        // Set up for forced game start
                        client.isPaired = true;
                        client.setTeam(chess_game.Pieces.Team.WHITE); // Requester gets WHITE
                        
                        // Ensure opponent name is set BEFORE creating game panel
                        setOpponentName(selectedPlayer.playerName);
                        
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try {
                                System.out.println("DEBUG: Force creating game panel for requester");
                                createGamePanel();
                                
                                // Send coordinated timer start message instead of manual start
                                // This ensures synchronization with the other player
                                javax.swing.Timer timerSyncDelay = new javax.swing.Timer(1500, timerEvent -> {
                                    try {
                                        System.out.println("DEBUG: Sending coordinated timer start for fallback scenario");
                                        Message timerStartMessage = new Message(Message.MessageTypes.TIMER_START);
                                        timerStartMessage.content = System.currentTimeMillis() + 500; // Start in 500ms
                                        client.Send(timerStartMessage);
                                        System.out.println("DEBUG: Coordinated timer start message sent");
                                    } catch (Exception timerEx) {
                                        // Fallback to manual start if message sending fails
                                        System.err.println("DEBUG: Timer message failed, using manual start: " + timerEx.getMessage());
                                        if (bottomGameMenu != null) {
                                            bottomGameMenu.startTimers();
                                        }
                                    }
                                    ((javax.swing.Timer) timerEvent.getSource()).stop();
                                });
                                timerSyncDelay.setRepeats(false);
                                timerSyncDelay.start();
                                
                            } catch (Exception ex) {
                                System.err.println("ERROR: Failed to force create game panel for requester: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        });
                        
                        // The timer will stop automatically after this execution since setRepeats(false)
                    });
                    requesterTimeoutTimer.setRepeats(false);
                    requesterTimeoutTimer.start();
                    
                    System.out.println("DEBUG: Request sent with 5s timeout timer started");
                } else {
                    JOptionPane.showMessageDialog(gameFrame, "Please select a player first!");
                }
            }
        });
        
        // Refresh button
        this.playerSelectionPanel.getRefreshBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Check if client is connected to server
                if (client == null || client.socket == null || client.socket.isClosed()) {
                    JOptionPane.showMessageDialog(gameFrame, 
                        "Not connected to server. Cannot refresh player list.", 
                        "Connection Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    Message msg = new Message(Message.MessageTypes.PLAYER_LIST);
                    msg.content = playerName;
                    client.Send(msg);
                    System.out.println("DEBUG: Player list refresh request sent for: " + playerName);
                    
                    // Update status to show refresh is in progress
                    playerSelectionPanel.getStatusLBL().setText("Refreshing player list...");
                } catch (Exception ex) {
                    System.err.println("ERROR: Failed to refresh player list: " + ex.getMessage());
                    JOptionPane.showMessageDialog(gameFrame, 
                        "Failed to refresh player list. Please try again.", 
                        "Network Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        // Back button
        this.playerSelectionPanel.getBackBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createMainMenu();
            }
        });
    }

    public void createGamePanel() {
        System.out.println("DEBUG: Creating game panel for multiplayer - START");
        System.out.println("DEBUG: Current thread: " + Thread.currentThread().getName());
        System.out.println("DEBUG: Client: " + (client != null ? "exists" : "null"));
        System.out.println("DEBUG: Player name: " + playerName);
        System.out.println("DEBUG: Opponent name: " + opponentName);
        System.out.println("DEBUG: Client paired state: " + (client != null ? client.isPaired : "N/A"));
        System.out.println("DEBUG: Game frame visible before: " + (gameFrame != null ? gameFrame.isVisible() : "frame null"));
        
        // Safety checks
        if (client == null) {
            String errorMsg = "ERROR: Client is null when creating game panel";
            System.err.println(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        System.out.println("DEBUG: Client team: " + client.getTeam());
        if (client.getTeam() == null) {
            String errorMsg = "ERROR: Client team is null when creating game panel - this should not happen";
            System.err.println(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        
        // Ensure we're on the EDT
        if (!javax.swing.SwingUtilities.isEventDispatchThread()) {
            System.out.println("DEBUG: Not on EDT, invoking later");
            javax.swing.SwingUtilities.invokeLater(() -> createGamePanel());
            return;
        }
        
        System.out.println("DEBUG: On EDT, proceeding with game panel creation");
        
        System.out.println("DEBUG: Removing all content from game frame");
        this.gameFrame.getContentPane().removeAll();
        
        // Force immediate removal and invalidation
        this.gameFrame.invalidate();
        this.gameFrame.revalidate();
        this.gameFrame.repaint();
        
        // Check if we have a loaded game state to use (only for actual load game requests)
        boolean isLoadedGame = (this.client != null && this.client.isWantToLoadGame() && this.client.getLoadedGameState() != null);
        
        System.out.println("DEBUG: Creating chess board - isLoadedGame: " + isLoadedGame);
        if (!isLoadedGame) {
            // Create fresh board for new game
            this.chessBoard = new Board();
            System.out.println("DEBUG: Created fresh board for new game");
        } else {
            // Use the loaded game state
            this.chessBoard = this.client.getLoadedGameState().getBoard();
            System.out.println("DEBUG: Using loaded game state - board loaded from saved game");
        }
        
        System.out.println("DEBUG: Creating board panel");
        this.boardPanel = new BoardPanel(this.chessBoard, this.client);
        this.boardPanel.setTable(this); // Set table reference for move highlighting
        
        System.out.println("DEBUG: Creating bottom game menu with team: " + this.client.getTeam());
        this.bottomGameMenu = new InGameBottomMenu(this.client.getTeam());

        this.bottomGameMenu.getPlayersColorLBL().setText("Your color is " + this.client.getTeam().toString());
        this.bottomGameMenu.setPlayerName(playerName != null ? playerName : "Player");
        System.out.println("DEBUG: Setting player name to: " + (playerName != null ? playerName : "Player"));

        // FIX: Use the opponent name that was set from StartInfo, ensuring it's properly preserved
String finalOpponentName = this.opponentName;
if (finalOpponentName != null && !finalOpponentName.isEmpty() && !finalOpponentName.equals(playerName)) {
    this.bottomGameMenu.setOpponentName(finalOpponentName);
    System.out.println("DEBUG: Setting opponent name from StartInfo to: " + finalOpponentName);
}

        setupChatHandlers();
        setupHintButtonHandler();

        // Reset move pending state for new game
        this.client.setMovePending(false);
        
        // Set initial turn display based on loaded game state or current player
        System.out.println("DEBUG: Client team is: " + this.client.getTeam() + ", setting turn display");
        System.out.println("DEBUG: Client movePending state reset to: " + this.client.isMovePending());
        
        if (isLoadedGame) {
            // For loaded games, check whose turn it is based on the loaded board state
            Team currentPlayerTeam = this.chessBoard.getCurrentPlayer().getTeam();
            if (this.client.getTeam() == currentPlayerTeam) {
                this.bottomGameMenu.getTurnLBL().setText("Your Turn");
                this.bottomGameMenu.getTurnLBL().setForeground(Color.GREEN);
                System.out.println("DEBUG: Loaded game - set turn display to 'Your Turn' for current player");
            } else {
                this.bottomGameMenu.getTurnLBL().setText("Enemy Turn");
                this.bottomGameMenu.getTurnLBL().setForeground(Color.RED);
                System.out.println("DEBUG: Loaded game - set turn display to 'Enemy Turn' for opponent");
            }
        } else {
            // For new games, use the standard logic
            if(this.client.getTeam() == Team.WHITE) {
                this.bottomGameMenu.getTurnLBL().setText("Your Turn");
                this.bottomGameMenu.getTurnLBL().setForeground(Color.GREEN);
                System.out.println("DEBUG: New game - set turn display to 'Your Turn' for WHITE team");
            } else {
                this.bottomGameMenu.getTurnLBL().setText("Enemy Turn");
                this.bottomGameMenu.getTurnLBL().setForeground(Color.RED);
                System.out.println("DEBUG: New game - set turn display to 'Enemy Turn' for non-WHITE team");
            }
        }

        JPanel boardWrapper = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        boardWrapper.setBackground(new Color(43, 43, 43));
        boardWrapper.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 0, 0, 0)); // Top offset: 100px
        boardWrapper.add(this.boardPanel);

        this.gameFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                boardPanel.revalidate();
                boardPanel.repaint();
            }
        });

        // ✅ Add wrapped board and bottom menu to the frame
        System.out.println("DEBUG: Adding components to game frame");
        this.gameFrame.add(boardWrapper, BorderLayout.CENTER);
        this.gameFrame.add(this.bottomGameMenu, BorderLayout.PAGE_END);

        System.out.println("DEBUG: Revalidating and repainting game frame");
        this.gameFrame.revalidate();
        this.gameFrame.repaint();
        
        // Force proper frame sizing and visibility
        System.out.println("DEBUG: Forcing frame pack and visibility");
        
        // Use SwingUtilities.invokeLater to ensure all UI updates are complete
        javax.swing.SwingUtilities.invokeLater(() -> {
            this.gameFrame.pack();
            this.gameFrame.setVisible(true);
            this.gameFrame.toFront();
            this.gameFrame.requestFocus();
            this.gameFrame.setState(java.awt.Frame.NORMAL); // Ensure window is not minimized
            
            // Additional debugging to check frame state
            System.out.println("DEBUG: Frame visible: " + this.gameFrame.isVisible());
            System.out.println("DEBUG: Frame size: " + this.gameFrame.getSize());
            System.out.println("DEBUG: Frame location: " + this.gameFrame.getLocation());
            System.out.println("DEBUG: Frame components count: " + this.gameFrame.getContentPane().getComponentCount());
            
            // Force another repaint after a short delay
            javax.swing.Timer refreshTimer = new javax.swing.Timer(200, e -> {
                System.out.println("DEBUG: Timer refresh - forcing final repaint");
                this.gameFrame.revalidate();
                this.gameFrame.repaint();
                this.gameFrame.toFront(); // Bring to front again
                ((javax.swing.Timer) e.getSource()).stop();
            });
            refreshTimer.setRepeats(false);
            refreshTimer.start();
        });
        
        // Final state verification
        System.out.println("DEBUG: Game panel creation completed successfully");
        System.out.println("DEBUG: Final frame state - visible: " + this.gameFrame.isVisible());
        System.out.println("DEBUG: Final frame state - focused: " + this.gameFrame.isFocused());
        System.out.println("DEBUG: Final frame state - size: " + this.gameFrame.getSize());
        System.out.println("DEBUG: Final frame content pane components: " + this.gameFrame.getContentPane().getComponentCount());
        
        // For new games, wait for server-coordinated TIMER_START message
        // For loaded games, skip auto-start as they will handle their own timer restoration
        if (!isLoadedGame) {
            System.out.println("DEBUG: New game created - waiting for server-coordinated timer start");
        } else {
            System.out.println("DEBUG: Skipping auto-timer start for loaded game - timers will be restored from save state");
        }
        
        if (!isLoadedGame) {
            System.out.println("DEBUG: Game panel created, waiting for server-coordinated timer start");
        } else {
            System.out.println("DEBUG: Game panel created for loaded game, timers will be restored from save state");
        }
        
        // List all components for debugging
        Component[] components = this.gameFrame.getContentPane().getComponents();
        for (int i = 0; i < components.length; i++) {
            System.out.println("DEBUG: Component " + i + ": " + components[i].getClass().getSimpleName());
        }
    }

    private void setupChatHandlers() {
        this.bottomGameMenu.getSendBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendChatMessage();
            }
        });
        
        this.bottomGameMenu.getChatInputTXT().addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendChatMessage();
                }
            }
            
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyReleased(KeyEvent e) {}
        });
        
        // Quit game button handler
        this.bottomGameMenu.getQuitGameBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = JOptionPane.showConfirmDialog(
                    gameFrame,
                    "Are you sure you want to quit the game?",
                    "Quit Game",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (response == JOptionPane.YES_OPTION) {
                    // Send leave message to server
                    Message leaveMsg = new Message(Message.MessageTypes.LEAVE);
                    client.Send(leaveMsg);
                    
                    // Reset client state
                    client.isPaired = false;
                    
                    // Return to main menu
                    createMainMenu();
                }
            }
        });
        
        // Save game button handler
        this.bottomGameMenu.getSaveGameBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String saveName = JOptionPane.showInputDialog(
                    gameFrame,
                    "Enter a name for this save:",
                    "Save Game",
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (saveName != null && !saveName.trim().isEmpty()) {
                    // Get timer state from bottom menu
                    int whiteTime = 0, blackTime = 0;
                    boolean timersActive = false;
                    boolean wasWhiteTurn = true;
                    
                    if (bottomGameMenu != null) {
                        whiteTime = bottomGameMenu.getWhiteTimeRemaining();
                        blackTime = bottomGameMenu.getBlackTimeRemaining();
                        timersActive = bottomGameMenu.areTimersRunning();
                        wasWhiteTurn = bottomGameMenu.isWhiteTurn();
                        
                        System.out.println("DEBUG: Saving timer state - White: " + whiteTime + "s, Black: " + blackTime + "s, Active: " + timersActive + ", WhiteTurn: " + wasWhiteTurn);
                    }
                    
                    // Create game state with player colors and timer state
                    GameState gameState = new GameState(
                        chessBoard,
                        playerName,
                        opponentName,
                        chessBoard.getCurrentPlayer().getTeam(),
                        saveName.trim(),
                        false, // This is a multiplayer game
                        client.getTeam(), // Player 1 color (current player)
                        (client.getTeam() == Team.WHITE) ? Team.BLACK : Team.WHITE, // Player 2 color (opponent)
                        whiteTime, // White timer remaining
                        blackTime, // Black timer remaining
                        timersActive, // Were timers active when saved
                        wasWhiteTurn // Whose turn it was when saved
                    );
                    
                    // Save locally using GameStateManager
                    if (chess_game.Utilities.GameStateManager.saveGame(gameState)) {
                        JOptionPane.showMessageDialog(
                            gameFrame,
                            "Game saved successfully as: " + saveName.trim(),
                            "Save Game",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        
                        // Also send to server if connected
                        if (client != null && client.isPaired) {
                            Message saveMsg = new Message(Message.MessageTypes.SAVE_GAME);
                            saveMsg.content = gameState;
                            client.Send(saveMsg);
                        }
                    } else {
                        JOptionPane.showMessageDialog(
                            gameFrame,
                            "Failed to save the game. Please try again.",
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        });
    }

    private void sendChatMessage() {
        String messageText = this.bottomGameMenu.getChatInputTXT().getText().trim();
        if (!messageText.isEmpty()) {
            ChatMessage chatMsg = new ChatMessage(playerName, messageText);
            
            Message msg = new Message(Message.MessageTypes.CHAT);
            msg.content = chatMsg;
            client.Send(msg);
            
            this.bottomGameMenu.addChatMessage("You: " + messageText);
            this.bottomGameMenu.getChatInputTXT().setText("");
        }
    }

    public void updatePlayersList(ArrayList<PlayerInfo> players) {
        System.out.println("DEBUG: updatePlayersList called with " + (players != null ? players.size() : "null") + " players");
        if (players != null) {
            for (PlayerInfo player : players) {
                System.out.println("DEBUG: Player: " + player.playerName + " (ID: " + player.playerId + ")");
            }
        }
        
        if (playerSelectionPanel != null) {
            playerSelectionPanel.updatePlayersList(players);
            System.out.println("DEBUG: Player list updated in playerSelectionPanel");
            
            // Update status message
            if (players == null || players.isEmpty()) {
                playerSelectionPanel.getStatusLBL().setText("No other players online. Wait for players to join or refresh the list.");
            } else {
                playerSelectionPanel.getStatusLBL().setText("Select a player and click 'Send Play Request'");
            }
        } else {
            System.out.println("DEBUG: playerSelectionPanel is null, cannot update player list");
        }
    }

    public void showPlayRequest(PlayRequest request) {
        System.out.println("DEBUG: Received play request from " + request.fromPlayerName);
        
        // Check if this is a load game request
        boolean isLoadGameRequest = request.isLoadGameRequest();
        String gameName = request.getLoadGameName();
        
        // Create appropriate message for the dialog
        String dialogMessage;
        String dialogTitle;
        
        if (isLoadGameRequest && gameName != null) {
            dialogMessage = request.fromPlayerName + " wants to continue a saved game with you.\n\n" +
                          "Game: " + gameName + "\n\n" +
                          "Accept this load game request?";
            dialogTitle = "Load Game Request";
        } else {
            dialogMessage = request.fromPlayerName + " wants to play with you. Accept?";
            dialogTitle = "Play Request";
        }
        
        int response = JOptionPane.showConfirmDialog(
            gameFrame,
            dialogMessage,
            dialogTitle,
            JOptionPane.YES_NO_OPTION
        );
        
        request.isAccepted = (response == JOptionPane.YES_OPTION);
        request.isRejected = (response == JOptionPane.NO_OPTION);
        
        System.out.println("DEBUG: Play request response - accepted: " + request.isAccepted + 
                          (isLoadGameRequest ? " (Load game: " + gameName + ")" : ""));
        
        // Create a new PlayRequest with swapped from/to fields for the response
        // This is crucial: the original request has requester as "from" and responder as "to"
        // The response needs to have responder as "from" and requester as "to" for proper routing
        PlayRequest responseRequest = new PlayRequest(
            playerName,                 // responder becomes "from"
            client.getClientId(),       // responder's ID becomes "fromPlayerId" 
            request.fromPlayerName,     // original requester becomes "to"
            request.fromPlayerId        // original requester's ID becomes "toPlayerId"
        );
        responseRequest.isAccepted = request.isAccepted;
        responseRequest.isRejected = request.isRejected;
        
        // Include load game information in the response
        if (isLoadGameRequest) {
            responseRequest.setLoadGameRequest(true);
            responseRequest.setLoadGameName(gameName);
            responseRequest.setLoadGameState(request.getLoadGameState());
        }
        
        System.out.println("DEBUG: Response details - fromPlayerName: " + responseRequest.fromPlayerName + ", fromPlayerId: " + responseRequest.fromPlayerId);
        System.out.println("DEBUG: Response details - toPlayerName: " + responseRequest.toPlayerName + ", toPlayerId: " + responseRequest.toPlayerId);
        
        Message msg = new Message(Message.MessageTypes.PLAY_RESPONSE);
        msg.content = responseRequest;
        client.Send(msg);
        
        // If request is accepted, prepare for game start
        if (request.isAccepted) {
            System.out.println("DEBUG: Request accepted by recipient - setting up for immediate game start");
            
            // FIX: Use the stored opponent name from PlayRequest instead of fromPlayerName
            // This ensures the correct opponent name is displayed
            String correctOpponentName = request.getResponderOpponentName(); // Who the responder sees as opponent
            if (correctOpponentName != null && !correctOpponentName.isEmpty()) {
                setOpponentName(correctOpponentName);
                System.out.println("DEBUG: Using stored opponent name from PlayRequest: " + correctOpponentName);
            } else {
                // Fallback to fromPlayerName if stored name is not available
                setOpponentName(request.fromPlayerName);
                System.out.println("DEBUG: Fallback to fromPlayerName: " + request.fromPlayerName);
            }
            
            // If this is a load game request, store the game state
            if (isLoadGameRequest && request.getLoadGameState() != null) {
                System.out.println("DEBUG: Storing loaded game state for accepted load game request");
                client.setLoadedGameState(request.getLoadGameState());
                
                // Also store the game state in the table for later use
                GameState gameState = request.getLoadGameState();
                this.chessBoard = gameState.getBoard();
                this.playerName = gameState.getPlayer1Name();
                this.opponentName = gameState.getPlayer2Name();
            }
            
            // Set client as paired since request was accepted
            client.isPaired = true;
            
            // Start a timer to force game creation if START message doesn't arrive
            javax.swing.Timer forceStartTimer = new javax.swing.Timer(3000, e -> {
                System.out.println("DEBUG: START message timeout - forcing game creation for responder with default team BLACK");
                
                // Set default team (responder gets BLACK) and force game creation
                if (client.getTeam() == null) {
                    client.setTeam(Team.BLACK);
                }
                
                javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        System.out.println("DEBUG: Force creating game panel for responder due to timeout");
                        createGamePanel();
                    } catch (Exception ex) {
                        System.err.println("ERROR: Failed to force create game panel for responder: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
                
                // Timer stops automatically since setRepeats(false)
            });
            forceStartTimer.setRepeats(false);
            forceStartTimer.start();
            
            // Auto-start the game timer when the game panel is created
            if (this.gameFrame != null && this.bottomGameMenu != null) {
                System.out.println("DEBUG: Auto-starting game timer");
                this.bottomGameMenu.startTimers();
            }
            
            System.out.println("DEBUG: Request accepted - waiting for START message (with 3s timeout)");
        }
    }
    
    /**
     * Show the load game dialog and handle game loading
     */
    private void showLoadGameDialog() {
        // Use the locked username from the session
        if (!usernameLockedForSession || registeredPlayerName == null) {
            JOptionPane.showMessageDialog(gameFrame, 
                "Username registration error. Please restart the application.", 
                "Registration Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String uniquePlayerName = registeredPlayerName;
        System.out.println("DEBUG: Load game dialog using locked username: " + uniquePlayerName);
        
        LoadGameDialog loadGameDialog = new LoadGameDialog(gameFrame, uniquePlayerName);
        loadGameDialog.setVisible(true);
        
        if (loadGameDialog.isGameLoaded()) {
            GameState loadedGameState = loadGameDialog.getLoadedGameState();
            
            // Load game state
            this.chessBoard = loadedGameState.getBoard();
            this.playerName = loadedGameState.getPlayer1Name();
            this.opponentName = loadedGameState.getPlayer2Name();
            
            // Determine if this is an AI game or multiplayer
            boolean isAIGame = loadedGameState.isAIGame();
            
            if (isAIGame) {
                // Determine player's color from saved game state
                Team playerTeam = Team.WHITE; // default
                
                if (loadedGameState.getPlayer1Color() != null) {
                    // Use saved player color information
                    if (playerName.equals(loadedGameState.getPlayer1Name())) {
                        playerTeam = loadedGameState.getPlayer1Color();
                    } else if (playerName.equals(loadedGameState.getPlayer2Name())) {
                        playerTeam = loadedGameState.getPlayer2Color();
                    }
                } else {
                    // Fallback: check AI name to determine player color
                    String opponentName = loadedGameState.getPlayer2Name();
                    if ("AI (White)".equals(opponentName)) {
                        playerTeam = Team.BLACK;
                    } else {
                        playerTeam = Team.WHITE; // AI (Black) or default
                    }
                }
                
                System.out.println("DEBUG: Loading AI game - player team: " + playerTeam);
                
                // Create AI game panel with correct player team and loaded board
                createAIGamePanel(playerTeam, loadedGameState.getBoard());
                
                // Display success message in status bar instead of popup to avoid dialog conflicts
                System.out.println("Game '" + loadedGameState.getSaveName() + "' loaded successfully. Playing with AI!");
            } else {
                // For multiplayer games, go directly to player selection page with NO popup
                if (client == null) {
                    try {
                        client = new Client(this);
                        // Set the team based on saved player colors
                        if (loadedGameState.getPlayer1Color() != null && loadedGameState.getPlayer2Color() != null) {
                            // Determine which player this client should be based on names
                            if (playerName.equals(loadedGameState.getPlayer1Name())) {
                                client.setTeam(loadedGameState.getPlayer1Color());
                            } else if (playerName.equals(loadedGameState.getPlayer2Name())) {
                                client.setTeam(loadedGameState.getPlayer2Color());
                            } else {
                                // Fallback to current player team if names don't match
                                client.setTeam(loadedGameState.getCurrentPlayerTeam());
                            }
                        } else {
                            // Fallback for old saves without color information
                            client.setTeam(loadedGameState.getCurrentPlayerTeam());
                        }
                        client.Connect("localhost", 4000);
                        if (client.socket != null && !client.socket.isClosed()) {
                            client.setLoadedGameState(loadedGameState);
                            createPlayerSelectionPanel();
                            if (playerSelectionPanel != null) {
                                playerSelectionPanel.getStatusLBL().setText("Select a player to continue saved game: " + loadedGameState.getSaveName());
                            }
                        } else {
                            JOptionPane.showMessageDialog(
                                gameFrame,
                                "Failed to connect to server.\nCannot load multiplayer game without connection.",
                                "Connection Error",
                                JOptionPane.ERROR_MESSAGE
                            );
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                            gameFrame,
                            "Error connecting to server: " + e.getMessage(),
                            "Connection Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                } else {
                    // Set the team based on saved player colors
                    if (loadedGameState.getPlayer1Color() != null && loadedGameState.getPlayer2Color() != null) {
                        // Determine which player this client should be based on names
                        if (playerName.equals(loadedGameState.getPlayer1Name())) {
                            client.setTeam(loadedGameState.getPlayer1Color());
                        } else if (playerName.equals(loadedGameState.getPlayer2Name())) {
                            client.setTeam(loadedGameState.getPlayer2Color());
                        } else {
                            // Fallback to current player team if names don't match
                            client.setTeam(loadedGameState.getCurrentPlayerTeam());
                        }
                    } else {
                        // Fallback for old saves without color information
                        client.setTeam(loadedGameState.getCurrentPlayerTeam());
                    }
                    client.setLoadedGameState(loadedGameState);
                    createPlayerSelectionPanel();
                    if (playerSelectionPanel != null) {
                        playerSelectionPanel.getStatusLBL().setText("Select a player to continue saved game: " + loadedGameState.getSaveName());
                    }
                }
            }
        }
    }
    
    // Backward compatibility method for createAIGamePanel
    public void createAIGamePanel() {
        createAIGamePanel(Team.WHITE); // Default to WHITE for backward compatibility
    }
    
    // Getters and setters
    public MainMenu getMainMenu() {
        return mainMenu;
    }

    public PlayerSelectionPanel getPlayerSelectionPanel() {
        return playerSelectionPanel;
    }

    public InGameBottomMenu getBottomGameMenu() {
        return bottomGameMenu;
    }

    public JFrame getGameFrame() {
        return gameFrame;
    }

    public BoardPanel getBoardPanel() {
        return boardPanel;
    }

    public Board getChessBoard() {
        return chessBoard;
    }

    public Client getClient() {
        return client;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getOpponentName() {
        return opponentName;
    }

    public void setOpponentName(String opponentName) {
        this.opponentName = opponentName;
        if (bottomGameMenu != null) {
            bottomGameMenu.setOpponentName(opponentName);
        }
    }

    public void setBoardPanel(BoardPanel boardPanel) {
        this.boardPanel = boardPanel;
    }

    public void setChessBoard(Board chessBoard) {
        this.chessBoard = chessBoard;
    }

    public void setGameFrame(JFrame gameFrame) {
        this.gameFrame = gameFrame;
    }

    public void setMainMenu(MainMenu mainMenu) {
        this.mainMenu = mainMenu;
    }

    public void setBottomGameMenu(InGameBottomMenu bottomGameMenu) {
        this.bottomGameMenu = bottomGameMenu;
    }

    public void setClient(Client client) {
        this.client = client;
    }
    
    public Move getLastMove() {
        return lastMove;
    }
    
    public void setLastMove(Move move) {
        this.lastMove = move;
    }

    // AI game panel creation with player team selection
    public void createAIGamePanel(Team playerTeam, Board loadedBoard) {
        this.gameFrame.getContentPane().removeAll();
        this.chessBoard = (loadedBoard != null) ? loadedBoard : new Board();
        this.boardPanel = new BoardPanel(this.chessBoard, null, playerTeam); // Use new constructor with player team
        this.boardPanel.setTable(this); // Set table reference for AI mode
        this.bottomGameMenu = new InGameBottomMenu(playerTeam, true);

        String playerColor = playerTeam == Team.WHITE ? "White" : "Black";
        String aiColor = playerTeam == Team.WHITE ? "Black" : "White";
        
        this.bottomGameMenu.getPlayersColorLBL().setText("Your color is " + playerColor);
        this.bottomGameMenu.setPlayerName(playerName);
        this.bottomGameMenu.setOpponentName("AI (" + aiColor + ")");

        JPanel boardWrapper = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        boardWrapper.setBackground(new Color(43, 43, 43));
        boardWrapper.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 0, 0, 0));
        boardWrapper.add(this.boardPanel);

        this.gameFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                boardPanel.revalidate();
                boardPanel.repaint();
            }
        });

        // Add button handlers for AI mode (reuse unified tutor system)
        setupAIGameButtonHandlers();
        setupHintButtonHandler(); // Use the same hint system as multiplayer
        
        this.gameFrame.add(boardWrapper, BorderLayout.CENTER);
        this.gameFrame.add(this.bottomGameMenu, BorderLayout.PAGE_END);
        this.gameFrame.revalidate();
        this.gameFrame.repaint();
        this.gameFrame.setVisible(true);

        // Add AI move trigger after each human move
        enableAIMoveAfterHuman(playerTeam);
        
        // If player chose BLACK, AI goes first - trigger AI's opening move
        if (playerTeam == Team.BLACK) {
            // Use a small delay to ensure UI is fully initialized
            javax.swing.Timer firstMoveTimer = new javax.swing.Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((javax.swing.Timer) e.getSource()).stop();
                    System.out.println("DEBUG: Player chose BLACK, triggering AI's first move");
                    executeAIMoveIfNeeded();
                }
            });
            firstMoveTimer.setRepeats(false);
            firstMoveTimer.start();
        }
    }

    // Existing method for backward compatibility
    public void createAIGamePanel(Team playerTeam) {
        createAIGamePanel(playerTeam, null);
    }

    // Helper to enable AI move after human move in AI mode
    private void enableAIMoveAfterHuman(Team playerTeam) {
        Team aiTeam = (playerTeam == Team.WHITE) ? Team.BLACK : Team.WHITE;
        
        // Store AI team and player team for reference
        this.aiTeam = aiTeam;
        this.humanTeam = playerTeam;
        
        System.out.println("DEBUG AI SETUP: humanTeam = " + this.humanTeam + ", aiTeam = " + this.aiTeam);
        System.out.println("DEBUG AI SETUP: Current board player = " + (chessBoard != null ? chessBoard.getCurrentPlayer().getTeam() : "null"));
        
        // Set initial turn display
        updateAITurnDisplay();
    }
    
    /**
     * Execute AI move after human move in AI mode
     * This should be called from TilePanel after a human move is completed
     */
    public void executeAIMoveIfNeeded() {
        // Only execute if we're in AI mode and it's the AI's turn
        if (aiTeam != null && chessBoard != null && 
            chessBoard.getCurrentPlayer().getTeam() == aiTeam && 
            !aiMoveInProgress) {
            
            aiMoveInProgress = true;
            updateAITurnDisplay();
            
            // Use a timer to add a small delay before AI move (better UX)
            javax.swing.Timer aiTimer = new javax.swing.Timer(800, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((javax.swing.Timer) e.getSource()).stop();
                    
                    SwingWorker<Move, Void> aiWorker = new SwingWorker<Move, Void>() {
                        @Override
                        protected Move doInBackground() throws Exception {
                            ChessAI ai = new ChessAI(3);
                            return ai.getBestMove(chessBoard, aiTeam);
                        }
                        
                        @Override
                        protected void done() {
                            try {
                                Move aiMove = get();
                                if (aiMove != null && chessBoard.getCurrentPlayer().getTeam() == aiTeam) {
                                // Execute the AI move
                                if (aiMove.hasKilledPiece() && bottomGameMenu != null) {
                                    bottomGameMenu.addCapturedPiece(
                                        aiMove.getKilledPiece().toString(),
                                        aiMove.getKilledPiece().getPoints()
                                    );
                                }
                                
                                // Make the move
                                chessBoard.getCurrentPlayer().makeMove(chessBoard, aiMove);
                                
                                // Track this as the last move
                                lastMove = aiMove;
                                
                                // Update board display
                                if (boardPanel != null) {
                                    boardPanel.updateBoardGUI(chessBoard);
                                }
                                
                                // Switch to human player
                                chessBoard.changeCurrentPlayer();
                                
                                // Update turn display
                                updateAITurnDisplay();
                                
                                // Check for game end
                                checkForGameEnding();
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            } finally {
                                aiMoveInProgress = false;
                            }
                        }
                    };
                    
                    aiWorker.execute();
                }
            });
            
            aiTimer.setRepeats(false);
            aiTimer.start();
        }
    }
    
    /**
     * Update turn display for AI mode
     */
    private void updateAITurnDisplay() {
        if (bottomGameMenu == null || chessBoard == null) return;
        
        Team currentTeam = chessBoard.getCurrentPlayer().getTeam();
        
        if (currentTeam == humanTeam) {
            bottomGameMenu.getTurnLBL().setText("Your Turn");
            bottomGameMenu.getTurnLBL().setForeground(Color.GREEN);
        } else if (currentTeam == aiTeam) {
            if (aiMoveInProgress) {
                bottomGameMenu.getTurnLBL().setText("AI is thinking...");
                bottomGameMenu.getTurnLBL().setForeground(Color.ORANGE);
            } else {
                bottomGameMenu.getTurnLBL().setText("AI Turn");
                bottomGameMenu.getTurnLBL().setForeground(Color.RED);
            }
        }
    }
    
    /**
     * Setup button handlers for AI mode
     */
    private void setupAIGameButtonHandlers() {
        // Quit game button handler for AI mode
        this.bottomGameMenu.getQuitGameBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = JOptionPane.showConfirmDialog(
                    gameFrame,
                    "Are you sure you want to quit the game?",
                    "Quit Game",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (response == JOptionPane.YES_OPTION) {
                    // Return to main menu
                    createMainMenu();
                }
            }
        });
        
        // Save game button handler for AI mode
        this.bottomGameMenu.getSaveGameBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String saveName = JOptionPane.showInputDialog(
                    gameFrame,
                    "Enter a name for this save:",
                    "Save Game",
                    JOptionPane.QUESTION_MESSAGE
                );
                
                if (saveName != null && !saveName.trim().isEmpty()) {
                    // Determine correct AI name based on actual AI team
                    String aiName = "AI (" + (aiTeam == Team.WHITE ? "White" : "Black") + ")";
                    
                    // Create game state with correct player and AI colors
                    Messages.GameState gameState = new Messages.GameState(
                        chessBoard,
                        playerName,
                        aiName,
                        chessBoard.getCurrentPlayer().getTeam(),
                        saveName.trim(),
                        true, // This is an AI game
                        humanTeam, // Player's actual color
                        aiTeam     // AI's actual color
                    );
                    
                    // Save using GameStateManager
                    boolean saved = chess_game.Utilities.GameStateManager.saveGame(gameState);
                    
                    if (saved) {
                        JOptionPane.showMessageDialog(
                            gameFrame,
                            "Game saved successfully as: " + saveName.trim() + "\n" +
                            "Save location: " + chess_game.Utilities.GameStateManager.getSaveDirectory(),
                            "Save Game",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    } else {
                        JOptionPane.showMessageDialog(
                            gameFrame,
                            "Failed to save the game. Please try again.",
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        });
    }
    
    /**
     * Setup hint button handler for both AI and multiplayer modes
     */
    private void setupHintButtonHandler() {
        this.bottomGameMenu.getHintBTN().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // No limit check needed - hints are unlimited
                showHint();
            }
        });
    }
    
    /**
     * Calculate and display the best move hint using ChessAI
     */
    private void showHint() {
        if (chessBoard == null) {
            JOptionPane.showMessageDialog(gameFrame, 
                "Game not ready for hints!", 
                "Hint Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Team currentTeam = chessBoard.getCurrentPlayer().getTeam();
        boolean isAIMode = (aiTeam != null); // AI mode is when aiTeam is set
        
        // In AI mode, allow hints anytime it's the human player's turn
        if (isAIMode) {
            // In AI mode, if it's the AI's turn or AI is currently moving, don't allow hints
            if (aiMoveInProgress) {
                JOptionPane.showMessageDialog(gameFrame, 
                    "Please wait for the AI to finish its move!", 
                    "AI Moving", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            // In AI mode, hints are always allowed when it's the human's turn
        } else {
            // Multiplayer mode - check if it's the player's turn
            if (client == null || client.getTeam() == null) {
                JOptionPane.showMessageDialog(gameFrame, 
                    "Game not ready for hints!", 
                    "Hint Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            Team playerTeam = client.getTeam();
            
            if (currentTeam != playerTeam) {
                JOptionPane.showMessageDialog(gameFrame, 
                    "Wait for your turn to get a hint!\nCurrent turn: " + currentTeam + "\nYour team: " + playerTeam, 
                    "Not Your Turn", 
                    JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }
        
        // Show "calculating" message
        bottomGameMenu.getHintBTN().setText("Thinking...");
        bottomGameMenu.getHintBTN().setEnabled(false);
        
        // Calculate hint in background thread
        SwingWorker<Move, Void> hintWorker = new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() throws Exception {
                ChessAI hintAI = new ChessAI(3); // Use same depth as game AI
                return hintAI.getBestMove(chessBoard, currentTeam);
            }
            
            @Override
            protected void done() {
                try {
                    Move bestMove = get();
                    
                    if (bestMove != null) {
                        displayHint(bestMove);
                        bottomGameMenu.useHint();
                    } else {
                        JOptionPane.showMessageDialog(gameFrame, 
                            "No good moves found!", 
                            "Hint", 
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(gameFrame, 
                        "Error calculating hint: " + ex.getMessage(), 
                        "Hint Error", 
                        JOptionPane.ERROR_MESSAGE);
                } finally {
                    // Reset button
                    bottomGameMenu.getHintBTN().setText("TUTOR");
                    bottomGameMenu.getHintBTN().setEnabled(true); // Always enabled since unlimited
                }
            }
        };
        
        hintWorker.execute();
    }
    
    /**
     * Display the hint to the user with move notation and strategic explanation
     */
    private void displayHint(Move bestMove) {
        String fromSquare = getSquareNotation(bestMove.getCurrentTile().getCoordinate());
        String toSquare = getSquareNotation(bestMove.getDestinationTile().getCoordinate());
        String pieceName = bestMove.getMovedPiece().getClass().getSimpleName();
        
        StringBuilder hintMessage = new StringBuilder();
        hintMessage.append("💡 Best Move Suggestion:\n\n");
        hintMessage.append("Move: ").append(pieceName).append(" from ").append(fromSquare)
                  .append(" to ").append(toSquare).append("\n\n");
        
        // Add strategic explanation
        String explanation = getMoveExplanation(bestMove);
        hintMessage.append("Strategy: ").append(explanation).append("\n\n");
        
        hintMessage.append("Tutor assistance is unlimited - use it anytime!");
        
        // Highlight the suggested move on the board
        highlightHintMove(bestMove);
        
        JOptionPane.showMessageDialog(gameFrame, 
            hintMessage.toString(), 
            "Chess Tutor - Hint", 
            JOptionPane.INFORMATION_MESSAGE);
        
        // Clear highlighting after a delay
        Timer clearHighlightTimer = new Timer(3000, e -> clearHintHighlight());
        clearHighlightTimer.setRepeats(false);
        clearHighlightTimer.start();
    }
    
    /**
     * Convert board coordinates to chess notation (e.g., e2, d4)
     */
    private String getSquareNotation(chess_game.Pieces.Coordinate coord) {
        char file = (char)('a' + coord.getX());
        int rank = 8 - coord.getY();
        return "" + file + rank;
    }
    
    /**
     * Generate strategic explanation for the suggested move
     */
    private String getMoveExplanation(Move move) {
        if (move.hasKilledPiece()) {
            String capturedPiece = move.getKilledPiece().getClass().getSimpleName().toLowerCase();
            return "Captures the opponent's " + capturedPiece + " - gaining material advantage!";
        }
        
        if (move.isPromotionMove()) {
            return "Promotes your pawn to a powerful piece - excellent endgame tactic!";
        }
        
        if (move.isCastlingMove()) {
            return "Castling move - improves king safety and develops the rook!";
        }
        
        String pieceName = move.getMovedPiece().getClass().getSimpleName().toLowerCase();
        
        // Check if move improves piece position
        int fromX = move.getCurrentTile().getCoordinate().getX();
        int fromY = move.getCurrentTile().getCoordinate().getY();
        int toX = move.getDestinationTile().getCoordinate().getX();
        int toY = move.getDestinationTile().getCoordinate().getY();
        
        // Check if moving toward center
        if (isMovingTowardCenter(fromX, fromY, toX, toY)) {
            return "Centralizes your " + pieceName + " - controlling key squares!";
        }
        
        // Check if piece is moving to safety
        if (isPieceCurrentlyHanging(move.getMovedPiece())) {
            return "Moves your " + pieceName + " to safety - avoiding capture!";
        }
        
        return "Improves your " + pieceName + " position - good strategic development!";
    }
    
    /**
     * Check if a move is toward the center of the board
     */
    private boolean isMovingTowardCenter(int fromX, int fromY, int toX, int toY) {
        double fromCenterDistance = Math.sqrt(Math.pow(fromX - 3.5, 2) + Math.pow(fromY - 3.5, 2));
        double toCenterDistance = Math.sqrt(Math.pow(toX - 3.5, 2) + Math.pow(toY - 3.5, 2));
        return toCenterDistance < fromCenterDistance;
    }
    
    /**
     * Check if a piece is currently hanging (can be captured)
     */
    private boolean isPieceCurrentlyHanging(chess_game.Pieces.Piece piece) {
        // This is a simplified check - in a full implementation, 
        // you'd check if the piece can be captured without adequate defense
        return false; // Placeholder
    }
    
    /**
     * Highlight the suggested move on the board
     */
    private void highlightHintMove(Move move) {
        if (boardPanel != null) {
            // Get display coordinates based on board rotation
            chess_game.Pieces.Coordinate fromCoord = boardPanel.getDisplayCoordinate(move.getCurrentTile().getCoordinate());
            chess_game.Pieces.Coordinate toCoord = boardPanel.getDisplayCoordinate(move.getDestinationTile().getCoordinate());
            
            // Highlight source and destination tiles
            TilePanel[][] tiles = boardPanel.getBoardTiles();
            
            if (fromCoord.getY() >= 0 && fromCoord.getY() < 8 && fromCoord.getX() >= 0 && fromCoord.getX() < 8) {
                tiles[fromCoord.getY()][fromCoord.getX()].setBackground(new Color(255, 255, 0, 150)); // Yellow highlight
            }
            
            if (toCoord.getY() >= 0 && toCoord.getY() < 8 && toCoord.getX() >= 0 && toCoord.getX() < 8) {
                tiles[toCoord.getY()][toCoord.getX()].setBackground(new Color(0, 255, 0, 150)); // Green highlight
            }
            
            boardPanel.repaint();
        }
    }
    
    /**
     * Clear hint highlighting from the board
     */
    private void clearHintHighlight() {
        if (boardPanel != null) {
            boardPanel.updateBoardGUI(chessBoard);
        }
    }
    
    /**
     * Check for checkmate or stalemate in AI mode and show appropriate popup
     * @return true if game has ended, false otherwise
     */
    private boolean checkForGameEnding() {
        return GameLogic.checkGameEnd(chessBoard, this, true);
    }
}
