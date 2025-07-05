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
                        Team serverChosenTeam = (Team) msg.content;
                        Team oldTeam = this.client.getTeam();
                        System.out.println("DEBUG: Received START message with team assignment: " + serverChosenTeam + " (was: " + oldTeam + ")");
                        
                        // Set team FIRST before any UI operations
                        this.client.setTeam(serverChosenTeam);
                        
                        // Mark client as paired (authoritative from server)
                        this.client.isPaired = true;
                        
                        // Create game panel - this is the authoritative start signal
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
                                
                                // Always create a fresh game panel when START is received
                                // This ensures both players start with identical game states
                                System.out.println("DEBUG: Creating fresh game panel from START message");
                                this.client.game.createGamePanel();
                                
                                // Additional UI thread operations to ensure proper display
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    try {
                                        // Force the game frame to be visible and in front
                                        if (this.client.game.getGameFrame() != null) {
                                            this.client.game.getGameFrame().setVisible(true);
                                            this.client.game.getGameFrame().toFront();
                                            this.client.game.getGameFrame().requestFocus();
                                            this.client.game.getGameFrame().repaint();
                                            System.out.println("DEBUG: Game frame forced to front and repainted");
                                        }
                                    } catch (Exception frameEx) {
                                        System.err.println("ERROR: Failed to make game frame visible: " + frameEx.getMessage());
                                        frameEx.printStackTrace();
                                    }
                                });
                                
                                System.out.println("DEBUG: START message processing completed - game should be ready");
                            } catch (Exception ex) {
                                System.err.println("ERROR: Failed to process START message: " + ex.getMessage());
                                ex.printStackTrace();
                                
                                // Reset client state on failure
                                this.client.isPaired = false;
                                this.client.setTeam(null);
                                
                                // Show error to user
                                JOptionPane.showMessageDialog(null, 
                                    "Failed to start game: " + ex.getMessage() + "\n\nReturning to main menu.",
                                    "Game Start Error", 
                                    JOptionPane.ERROR_MESSAGE);
                                
                                // Return to main menu on error
                                try {
                                    this.client.game.createMainMenu();
                                } catch (Exception menuEx) {
                                    System.err.println("ERROR: Failed to return to main menu: " + menuEx.getMessage());
                                }
                            }
                        });
                        break;
                        
                        
                    case PLAYER_LIST:
                        ArrayList<PlayerInfo> players = (ArrayList<PlayerInfo>) msg.content;
                        this.client.game.updatePlayersList(players);
                        break;
                        
                    case PLAY_REQUEST:
                        PlayRequest request = (PlayRequest) msg.content;
                        this.client.game.showPlayRequest(request);
                        break;
                        
                    case PLAY_RESPONSE:
                        PlayRequest response = (PlayRequest) msg.content;
                        System.out.println("DEBUG: Received PLAY_RESPONSE - accepted: " + response.isAccepted + ", from: " + response.fromPlayerName);
                        if (response.isAccepted) {
                            System.out.println("DEBUG: Play request accepted by " + response.fromPlayerName + " - setting up for game start");
                            
                            // Store opponent name and prepare for game
                            this.client.game.setOpponentName(response.fromPlayerName);
                            
                            // Set client as paired since request was accepted
                            this.client.isPaired = true;
                            
                            // Start a timer to force game creation if START message doesn't arrive
                            javax.swing.Timer forceStartTimer = new javax.swing.Timer(3000, e -> {
                                System.out.println("DEBUG: START message timeout - forcing game creation with default team WHITE");
                                
                                // Set default team and force game creation
                                if (this.client.getTeam() == null) {
                                    this.client.setTeam(chess_game.Pieces.Team.WHITE);
                                }
                                
                                javax.swing.SwingUtilities.invokeLater(() -> {
                                    try {
                                        System.out.println("DEBUG: Force creating game panel due to timeout");
                                        this.client.game.createGamePanel();
                                    } catch (Exception ex) {
                                        System.err.println("ERROR: Failed to force create game panel: " + ex.getMessage());
                                        ex.printStackTrace();
                                    }
                                });
                                
                                ((javax.swing.Timer) e.getSource()).stop();
                            });
                            forceStartTimer.setRepeats(false);
                            forceStartTimer.start();
                            
                            System.out.println("DEBUG: Request accepted - waiting for START message (with 3s timeout)");
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
                                this.client.game.getBottomGameMenu().switchTimerTurn();
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
                        // Handle load game request
                        if (msg.content instanceof GameState) {
                            GameState loadedGame = (GameState) msg.content;
                            // Load the game state
                            loadGameState(loadedGame);
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
                            
                            // Calculate delay to synchronize with server timestamp
                            long currentTime = System.currentTimeMillis();
                            long delay = Math.max(0, serverTimestamp + 100 - currentTime); // Small buffer
                            
                            javax.swing.Timer syncedStart = new javax.swing.Timer((int)delay, e -> {
                                this.client.game.getBottomGameMenu().startTimers();
                                ((javax.swing.Timer) e.getSource()).stop();
                                System.out.println("DEBUG: Synchronized timer started");
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
            
            // Update player names
            this.client.game.setPlayerName(gameState.getPlayer1Name());
            this.client.game.setOpponentName(gameState.getPlayer2Name());
            
            // Update the board panel
            this.client.game.getBoardPanel().updateBoardGUI(gameState.getBoard());
            
            // Update the bottom menu
            if (this.client.game.getBottomGameMenu() != null) {
                this.client.game.getBottomGameMenu().setPlayerName(gameState.getPlayer1Name());
                this.client.game.getBottomGameMenu().setOpponentName(gameState.getPlayer2Name());
                
                // Update turn indicator
                if (gameState.getCurrentPlayerTeam() == this.client.getTeam()) {
                    this.client.game.getBottomGameMenu().getTurnLBL().setText("Your Turn");
                    this.client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.GREEN);
                } else {
                    this.client.game.getBottomGameMenu().getTurnLBL().setText("Enemy Turn");
                    this.client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.RED);
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
