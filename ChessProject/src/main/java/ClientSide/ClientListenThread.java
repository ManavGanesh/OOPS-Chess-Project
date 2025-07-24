package ClientSide;

import Messages.ChatMessage;
import Messages.Message;
import Messages.PlayerInfo;
import Messages.PlayRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import Messages.MovementMessage;
import chess_game.Boards.Board;
import chess_game.Move.Move;
import chess_game.Pieces.PieceTypes;
import chess_game.Pieces.Team;
import chess_game.Player.Player;
import java.awt.Color;
import javax.swing.JOptionPane;
import Messages.GameState;
import server.StartInfo;

public class ClientListenThread extends Thread {

    Client client;

    public ClientListenThread(Client client) {
        this.client = client;
    }

    @Override
    public void run() {
        while (!this.client.socket.isClosed()) {
            try {
                Message msg = (Message) (this.client.sInput.readObject());
                switch (msg.type) {
                    case START:
                        // Handle game start message
                        System.out.println("DEBUG: Received START message from server");
                        
                        // Handle both old Team format and new StartInfo format for backward compatibility
                        if (msg.content instanceof StartInfo) {
                            StartInfo startInfo = (StartInfo) msg.content;
                            this.client.setTeam(startInfo.getTeam());
                            
                            // Use the names from StartInfo as they now contain the correct display names
                            this.client.game.setPlayerName(startInfo.getPlayerName());
                            this.client.game.setOpponentName(startInfo.getOpponentName());
                            
                            System.out.println("DEBUG: StartInfo - Player: " + this.client.game.getPlayerName() + 
                                             " (" + startInfo.getTeam() + ") vs " + this.client.game.getOpponentName());
                        } else if (msg.content instanceof chess_game.Pieces.Team) {
                            // Legacy support for old Team-only format
                            this.client.setTeam((chess_game.Pieces.Team) msg.content);
                            System.out.println("DEBUG: Legacy Team format - Client team set to: " + this.client.getTeam());
                        }
                        
                        // Set client as paired
                        this.client.isPaired = true;
                        
                        // Execute on EDT to ensure thread safety
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            try {
                                System.out.println("DEBUG: Processing START message - creating game panel");
                                System.out.println("DEBUG: Client team set to: " + this.client.getTeam());
                                System.out.println("DEBUG: Client paired state: " + this.client.isPaired);
                                
                                // Show a brief connection status if we're still in player selection
                                if (this.client.game.getPlayerSelectionPanel() != null) {
                                    this.client.game.getPlayerSelectionPanel().getStatusLBL().setText("Connected! Starting game...");
                                    System.out.println("DEBUG: Updated player selection status");
                                }
                                
                                // Verify client state before creating game panel
                                if (this.client.getTeam() == null) {
                                    throw new IllegalStateException("Client team is null after setting from START message");
                                }
                                
                                // Check if we have a loaded game state to apply
                                if (this.client.game.getChessBoard() != null && 
                                    this.client.game.getPlayerName() != null && 
                                    this.client.game.getOpponentName() != null) {
                                    // We have a loaded game state, apply it before creating the panel
                                    System.out.println("DEBUG: Applying loaded game state before creating game panel");
                                    this.client.game.getBoardPanel().updateBoardGUI(this.client.game.getChessBoard());
                                }
                                
                                // Always create a fresh game panel when START is received
                                // This ensures both players start with identical game states
                                System.out.println("DEBUG: Creating fresh game panel from START message");
                                this.client.game.createGamePanel();
                                
                                // Additional UI thread operations to ensure proper display
                                this.client.game.getGameFrame().revalidate();
                                this.client.game.getGameFrame().repaint();
                                
                                System.out.println("DEBUG: Game panel created successfully from START message");
                                
                            } catch (Exception ex) {
                                System.err.println("ERROR: Failed to create game panel from START message: " + ex.getMessage());
                                ex.printStackTrace();
                            }
                        });
                        break;
                        
                        
                    case PLAYER_LIST:
                        @SuppressWarnings("unchecked")
                        ArrayList<PlayerInfo> players = (ArrayList<PlayerInfo>) msg.content;
                        System.out.println("DEBUG: Client received player list with " + (players != null ? players.size() : "null") + " players");
                        if (players != null) {
                            for (PlayerInfo player : players) {
                                System.out.println("DEBUG: Received player: " + player.playerName + " (ID: " + player.playerId + ")");
                            }
                        }
                        this.client.game.updatePlayersList(players);
                        break;
                        
                    case PAIRING:
                        // Handle pairing message with opponent name
                        String opponentName = (String) msg.content;
                        System.out.println("DEBUG: Received PAIRING message - opponent: " + opponentName);
                        this.client.game.setOpponentName(opponentName);
                        break;
                        
                    case PLAY_REQUEST:
                        PlayRequest request = (PlayRequest) msg.content;
                        System.out.println("DEBUG: Received play request from " + request.fromPlayerName);
                        this.client.game.showPlayRequest(request);
                        break;
                        
                    case PLAY_RESPONSE:
                        PlayRequest response = (PlayRequest) msg.content;
                        System.out.println("DEBUG: Received PLAY_RESPONSE - accepted: " + response.isAccepted + ", from: " + response.fromPlayerName);
                        if (response.isAccepted) {
                            System.out.println("DEBUG: Play request accepted by " + response.fromPlayerName + " - setting up for game start");
                            
                            // FIX: Use stored opponent name from PlayRequest instead of fromPlayerName
                            // This ensures the correct opponent name is displayed for the requester
                            String correctOpponentName = response.getRequesterOpponentName(); // Who the requester sees as opponent
                            if (correctOpponentName != null && !correctOpponentName.isEmpty()) {
                                this.client.game.setOpponentName(correctOpponentName);
                                System.out.println("DEBUG: Using stored opponent name from PlayRequest: " + correctOpponentName);
                            } else {
                                // Fallback to fromPlayerName if stored name is not available
                                this.client.game.setOpponentName(response.fromPlayerName);
                                System.out.println("DEBUG: Fallback to fromPlayerName: " + response.fromPlayerName);
                            }
                            
                            // Set client as paired since request was accepted
                            this.client.isPaired = true;
                            
                            // DISABLED: Remove the force start timer that causes multiple game panel creation
                            // The server should send a proper START message with team assignment
                            // If START message doesn't arrive, the game shouldn't start with wrong team assignment
                            
                            System.out.println("DEBUG: Request accepted - waiting for START message from server");
                        } else {
                            System.out.println("DEBUG: Play request declined by " + response.fromPlayerName);
                            
                            // Show declined message and return to main menu
                            JOptionPane.showMessageDialog(null, response.fromPlayerName + " declined your request.");
                            
                            // Reset client state and re-enable the send request button
                            this.client.isPaired = false;
                            if (this.client.game.getPlayerSelectionPanel() != null) {
                                this.client.game.getPlayerSelectionPanel().getSendRequestBTN().setEnabled(true);
                                this.client.game.getPlayerSelectionPanel().getStatusLBL().setText("Request declined.");
                            }
                        }
                        break;
                        
                    case REQUEST_DENIED:
                        String requestMessage = (String) msg.content;  // Changed variable name
                        JOptionPane.showMessageDialog(null, requestMessage);
                        // Reset UI state properly
                        if (this.client.game.getPlayerSelectionPanel() != null) {
                            this.client.game.getPlayerSelectionPanel().getSendRequestBTN().setEnabled(true);
                            this.client.game.getPlayerSelectionPanel().getStatusLBL().setText("Player not available.");
                        }
                        break;
                        
                    case CHAT:
                        ChatMessage chatMessage = (ChatMessage) msg.content;
                        String displayMessage = chatMessage.playerName + ": " + chatMessage.message;
                        if (this.client.game.getBottomGameMenu() != null) {
                            this.client.game.getBottomGameMenu().addChatMessage(displayMessage);
                        }
                        break;
                        
                    case MOVE:
                        MovementMessage movement = (MovementMessage) msg.content;
                        Board board = this.client.game.getChessBoard();
                        Player player = board.getCurrentPlayer();
                        Move move;
                        if (movement.isCastling) {
                            move = new Move(
                                board,
                                board.getTile(movement.currentCoordinate),
                                board.getTile(movement.destinationCoordinate),
                                board.getTile(movement.rookStartCoordinate),
                                board.getTile(movement.rookEndCoordinate)
                            );
                        } else if (movement.isEnPassant) {
                            move = new Move(
                                board,
                                board.getTile(movement.currentCoordinate),
                                board.getTile(movement.destinationCoordinate),
                                board.getTile(movement.enPassantCapturedPawnCoordinate)
                            );
                        } else if (movement.isPromotion) {
                            move = new Move(
                                board,
                                board.getTile(movement.currentCoordinate),
                                board.getTile(movement.destinationCoordinate)
                            );
                            move.setPromotionMove(true);
                            move.setPromotionPieceType(PieceTypes.valueOf(movement.promotionPieceType));
                        } else {
                            move = new Move(board, board.getTile(movement.currentCoordinate), board.getTile(movement.destinationCoordinate));
                        }
                        // Apply all moves received from the server (both our validated moves and opponent moves)
                        // The server only sends moves that have been validated
                        player.makeMove(board, move);
                        
                        // Track captured pieces for both our moves and opponent moves
                        if (move.hasKilledPiece() || move.isEnPassantMove()) {
                            if (this.client.game.getBottomGameMenu() != null) {
                                // For en passant, the killed piece might be stored differently
                                if (move.isEnPassantMove() && move.getKilledPiece() != null) {
                                    this.client.game.getBottomGameMenu().addCapturedPiece(
                                        move.getKilledPiece().toString(),
                                        move.getKilledPiece().getPoints()
                                    );
                                } else if (move.hasKilledPiece()) {
                                    this.client.game.getBottomGameMenu().addCapturedPiece(
                                        move.getKilledPiece().toString(),
                                        move.getKilledPiece().getPoints()
                                    );
                                }
                                
                                // Check for king capture (game ending)
                                if (move.hasKilledPiece() && move.getKilledPiece().getType() == PieceTypes.KING) {
                                    Team winnerTeam;
                                    winnerTeam = (move.getKilledPiece().getTeam() == Team.BLACK) ? Team.WHITE : Team.BLACK;
                                    JOptionPane.showMessageDialog(null, "Winner: " + winnerTeam.toString());
                                    Message endMessage = new Message(Message.MessageTypes.END);
                                    endMessage.content = null;
                                    client.Send(endMessage);
                                    break;
                                }
                            }
                        }
                        
                        // Always change the current player to reflect the server's turn management
                        board.changeCurrentPlayer();
                        
                        // Cancel the move timeout timer since we received the move
                        if (this.client.isMovePending()) {
                            this.client.moveTimeoutTimer.cancel();
                        }
                        
                        // Clear the move pending state since we received a server response
                        this.client.setMovePending(false);
                        
                        // Update last move for highlighting
                        if (this.client.game != null) {
                            this.client.game.setLastMove(move);
                        }
                        
                        // Update GUI
                        this.client.game.getBoardPanel().updateBoardGUI(this.client.game.getChessBoard());
                        
                        // Check for game ending conditions (checkmate/stalemate) after move
                        if (chess_game.Utilities.GameLogic.checkGameEnd(board, this.client.game, false)) {
                            // Game has ended, don't continue with turn updates
                            break;
                        }
                        
                        // Update turn indicator based on whose turn it is now
                        if (this.client.game.getBottomGameMenu() != null) {
                            if (board.getCurrentPlayer().getTeam() == this.client.getTeam()) {
                                this.client.game.getBottomGameMenu().getTurnLBL().setText("Your Turn");
                                this.client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.GREEN);
                            } else {
                                this.client.game.getBottomGameMenu().getTurnLBL().setText("Enemy Turn");
                                this.client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.RED);
                            }
                            
                            // Improved timer synchronization: add small delay to ensure both clients switch simultaneously
                            javax.swing.Timer timerSyncDelay = new javax.swing.Timer(100, e -> {
                                if (this.client.game.getBottomGameMenu().areTimersRunning()) {
                                    this.client.game.getBottomGameMenu().switchTimerTurn();
                                    System.out.println("DEBUG: Timer switched after move - White: " + 
                                                      this.client.game.getBottomGameMenu().getWhiteTimeRemaining() + "s, Black: " + 
                                                      this.client.game.getBottomGameMenu().getBlackTimeRemaining() + "s");
                                }
                                ((javax.swing.Timer) e.getSource()).stop();
                            });
                            timerSyncDelay.setRepeats(false);
                            timerSyncDelay.start();
                        }
                        break;
                        
                    case CHECK:
                        Team checkStateTeam = (Team) msg.content;
                        // Removed popup - check state is handled silently
                        break;
                        
                    case CHECKMATE:
                        Team checkmateTeam = (Team) msg.content;
                        JOptionPane.showMessageDialog(null, "Checkmate! " + checkmateTeam.toString() + " wins!");
                        this.client.isPaired = false;
                        this.client.game.createMainMenu();
                        break;
                        
                    case STALEMATE:
                        JOptionPane.showMessageDialog(null, "Stalemate! The game is a draw.");
                        this.client.isPaired = false;
                        this.client.game.createMainMenu();
                        break;
                        
                    case SAVE_GAME:
                        // Handle save game confirmation from server
                        if (msg.content instanceof GameState) {
                            GameState savedGame = (GameState) msg.content;
                            System.out.println("Game saved successfully: " + savedGame.getSaveName());
                        }
                        break;
                        
                    case LOAD_GAME:
                        // Handle load game request/response
                        if (msg.content instanceof GameState) {
                            GameState loadedGame = (GameState) msg.content;
                            System.out.println("Received load game request/response for: " + loadedGame.getSaveName());
                            
                            // Load the game state
                            loadGameState(loadedGame);
                            
                            // If this is a multiplayer game and we're not paired, try to pair
                            if (!loadedGame.isAIGame() && !this.client.isPaired) {
                                System.out.println("Attempting to pair for multiplayer load game");
                                // The pairing logic will handle this automatically
                            }
                        }
                        break;

                    case END:
                        // Handle game ending (checkmate or king capture)
                        this.client.isPaired = false;
                        if (msg.content != null) {
                            // This is a checkmate
                            String winner = msg.content.toString();
                            JOptionPane.showMessageDialog(null, "Game Over! " + winner + " wins!");
                        }
                        this.client.game.createMainMenu();
                        break;

                    case TIMER_SYNC:
                        // Handle timer synchronization message
                        if (this.client.game.getBottomGameMenu() != null) {
                            System.out.println("DEBUG: Received timer sync message - switching timer turn");
                            this.client.game.getBottomGameMenu().switchTimerTurn();
                        }
                        break;
                        
                    case TIMER_START:
                        // Handle synchronized timer start from server
                        if (this.client.game.getBottomGameMenu() != null) {
                            Long serverTimestamp = (Long) msg.content;
                            System.out.println("DEBUG: Received synchronized timer start from server");
                            
                            // FIX: Ensure opponent name is preserved from StartInfo without fallback override
                            // The opponent name should already be set correctly from the START message
                            String currentOpponentName = this.client.game.getOpponentName();
                            if (currentOpponentName != null && !currentOpponentName.isEmpty()) {
                                System.out.println("DEBUG: Confirming opponent name before timer start: " + currentOpponentName);
                                // Only set if bottomGameMenu exists and opponent name is valid
                                if (this.client.game.getBottomGameMenu() != null) {
                                    this.client.game.getBottomGameMenu().setOpponentName(currentOpponentName);
                                }
                            } else {
                                System.out.println("DEBUG: WARNING - Opponent name is null or empty before timer start, but NOT overriding with fallback");
                                // Don't override with fallback - let the StartInfo opponent name be preserved
                            }
                            
                            // Calculate delay to synchronize with server timestamp
                            long currentTime = System.currentTimeMillis();
                            long delay = Math.max(500, serverTimestamp - currentTime); // Ensure minimum 500ms delay
                            
                            javax.swing.Timer syncedStart = new javax.swing.Timer((int)delay, e -> {
                                // Check if this is a loaded game timer restart or new game timer start
                                if (this.client.game.getBottomGameMenu().getWhiteTimeRemaining() != 600 || 
                                    this.client.game.getBottomGameMenu().getBlackTimeRemaining() != 600) {
                                    // This is a loaded game - restart timers with saved state
                                    this.client.game.getBottomGameMenu().restartTimersAfterLoad(true);
                                    System.out.println("DEBUG: Synchronized timer restarted for loaded game - White: " + 
                                                      this.client.game.getBottomGameMenu().getWhiteTimeRemaining() + "s, Black: " + 
                                                      this.client.game.getBottomGameMenu().getBlackTimeRemaining() + "s");
                                } else {
                                    // This is a new game - start fresh timers
                                    this.client.game.getBottomGameMenu().startTimers();
                                    System.out.println("DEBUG: Synchronized timer started for new game");
                                }
                                ((javax.swing.Timer) e.getSource()).stop();
                            });
                            syncedStart.setRepeats(false);
                            syncedStart.start();
                        }
                        break;
                        
                    case LEAVE:
                        JOptionPane.showMessageDialog(null, "Enemy left. Returning to the Menu.");
                        this.client.isPaired = false;
                        this.client.game.createMainMenu();
                        break;
                        
                    case USERNAME_REGISTER_RESPONSE:
                        Messages.UsernameRegisterMessage registerResponse = (Messages.UsernameRegisterMessage) msg.content;
                        this.client.game.handleUsernameRegistrationResponse(registerResponse);
                        break;
                        
                    case ERROR:
                        String errorMessage = (String) msg.content;
                        System.out.println("ERROR from server: " + errorMessage);
                        
                        // Cancel the move timeout timer since we received an error response
                        if (this.client.isMovePending()) {
                            this.client.moveTimeoutTimer.cancel();
                        }
                        
                        // Clear the move pending state since we received an error response
                        this.client.setMovePending(false);
                        
                        // Reset the UI state when an error occurs
                        Board errorBoard = this.client.game.getChessBoard();
                        if (errorBoard != null) {
                            // Clear any chosen tile to reset the selection
                            errorBoard.setChosenTile(null);
                            
                            // Update the board GUI to reflect the reset
                            this.client.game.getBoardPanel().updateBoardGUI(errorBoard);
                        }
                        
                        // Reset turn indicator to correct state
                        if (this.client.game.getBottomGameMenu() != null) {
                            if (errorBoard != null && errorBoard.getCurrentPlayer().getTeam() == this.client.getTeam()) {
                                this.client.game.getBottomGameMenu().getTurnLBL().setText("Your Turn");
                                this.client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.GREEN);
                            } else {
                                this.client.game.getBottomGameMenu().getTurnLBL().setText("Enemy Turn");
                                this.client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.RED);
                            }
                        }
                        
                        // Show error message to user
                        JOptionPane.showMessageDialog(null, "Server Error: " + errorMessage, "Turn Error", JOptionPane.ERROR_MESSAGE);
                        break;
                        
                    default:
                        System.out.println("Unknown message type received: " + msg.type);
                        break;
                }

            } catch (IOException ex) {
                Logger.getLogger(ClientListenThread.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Connection lost to server");
                break;
            } catch (ClassNotFoundException ex) {
                System.out.println("Girilen class bulunamadı: " + ex.getMessage());
                break;
            } catch (Exception ex) {
                System.out.println("Unexpected error in ClientListenThread: " + ex.getMessage());
                ex.printStackTrace();
                break;
            }
        }
        
        try {
            if (this.client.socket != null && !this.client.socket.isClosed()) {
                this.client.socket.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(ClientListenThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void loadGameState(GameState gameState) {
        try {
            // Update the game board with the loaded state
            this.client.game.setChessBoard(gameState.getBoard());
            
            // Correctly assign player names based on client's team
            String playerName;
            String opponentName;
            
            if (this.client.getTeam() == Team.WHITE) {
                // Client is WHITE team
                playerName = gameState.getPlayer1Name();  // Player1 is WHITE
                opponentName = gameState.getPlayer2Name(); // Player2 is BLACK
            } else {
                // Client is BLACK team
                playerName = gameState.getPlayer2Name();  // Player2 is BLACK
                opponentName = gameState.getPlayer1Name(); // Player1 is WHITE
            }
            
            // Update player names
            this.client.game.setPlayerName(playerName);
            this.client.game.setOpponentName(opponentName);
            
            // Update the board panel
            this.client.game.getBoardPanel().updateBoardGUI(gameState.getBoard());
            
            // Update the bottom menu
            if (this.client.game.getBottomGameMenu() != null) {
                this.client.game.getBottomGameMenu().setPlayerName(playerName);
                this.client.game.getBottomGameMenu().setOpponentName(opponentName);
                
                // Update turn indicator
                if (gameState.getCurrentPlayerTeam() == this.client.getTeam()) {
                    this.client.game.getBottomGameMenu().getTurnLBL().setText("Your Turn");
                    this.client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.GREEN);
                } else {
                    this.client.game.getBottomGameMenu().getTurnLBL().setText("Enemy Turn");
                    this.client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.RED);
                }
                
                // Restore timer state if available
                if (gameState.hasTimerData()) {
                    System.out.println("DEBUG: Loading timer data - White: " + gameState.getWhiteTimeRemaining() + "s, Black: " + gameState.getBlackTimeRemaining() + "s, Active: " + gameState.getTimersWereActive() + ", WhiteTurn: " + gameState.getWasWhiteTurn());
                    
                    this.client.game.getBottomGameMenu().setTimerState(
                        gameState.getWhiteTimeRemaining(),
                        gameState.getBlackTimeRemaining(),
                        gameState.getTimersWereActive(),
                        gameState.getWasWhiteTurn()
                    );
                    
                    // Always start timers for loaded games (they should continue from where they left off)
                    // Schedule a synchronized timer restart
                    javax.swing.Timer syncTimer = new javax.swing.Timer(1500, e -> {
                        long startTime = System.currentTimeMillis() + 1000; // Start in 1 second
                        
                        Messages.Message timerRestartMessage = new Messages.Message(Messages.Message.MessageTypes.TIMER_START);
                        timerRestartMessage.content = startTime;
                        
                        // Send to server for coordination
                        this.client.Send(timerRestartMessage);
                        
                        System.out.println("DEBUG: Sent timer restart message for loaded game");
                        ((javax.swing.Timer) e.getSource()).stop();
                    });
                    syncTimer.setRepeats(false);
                    syncTimer.start();
                }
            }
            
            JOptionPane.showMessageDialog(null, 
                "Game loaded successfully: " + gameState.getSaveName(),
                "Load Game", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (Exception ex) {
            System.out.println("Error loading game state: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(null, 
                "Error loading game: " + ex.getMessage(),
                "Load Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
