/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import Messages.Message;
import chess_game.Pieces.Team;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */

//The purpose of this thread is the pairing every client with its rival in game. Because of the chess game is 1 versus 1 game,
// the pairing operation just happens between 2 clients. When this thread called by a sclient, it starts to search for a player 
// which also want to pair with someone. When the pairing completed, this thread gonna stops until this sclient finish or leave
//its match and start to search a new rival for a new game.
public class ClientPairingThread extends Thread {

    SClient client;

    public ClientPairingThread(SClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        while (this.client.socket.isConnected() && 
               (this.client.isWantToPair == true || this.client.isWantToLoadGame == true) && 
               this.client.isPaired == false) {

            try {
                // take one client to here ==> acquire 1 permit
                Server.pairingLockForTwoPair.acquire(1);
                //matching system starts to matching clients.
                SClient chosenPair = null;
                //while the client is connected and not have pair try this to match him.
                while (this.client.socket.isConnected() && chosenPair == null && 
                       (this.client.isWantToPair || this.client.isWantToLoadGame)) {
                    synchronized (Server.clients) {
                        for (SClient client : Server.clients) {
                            if (client != this.client && client.isPaired == false && 
                                (client.isWantToPair == true || client.isWantToLoadGame == true)) {
                                
                                // Check if both clients want to load games
                                boolean bothWantToLoad = this.client.isWantToLoadGame && client.isWantToLoadGame;
                                boolean oneWantsToLoad = this.client.isWantToLoadGame || client.isWantToLoadGame;
                                
                                // If one wants to load but the other doesn't, skip this pairing
                                if (oneWantsToLoad && !bothWantToLoad) {
                                    continue;
                                }
                                //matching objects and making client pairs to play each other.
                                final SClient foundPair = client;
                                this.client.pair = foundPair;
                                foundPair.pair = this.client;
                                this.client.isWantToPair = false;
                                this.client.isWantToLoadGame = false;
                                chosenPair = foundPair;
                                client.isWantToPair = false;
                                client.isWantToLoadGame = false;
                                client.isPaired = true;
                                this.client.isPaired = true;
                                
                                //giving information to the clients about the success on pairing
                                // Send opponent names to each client
                                Message pairingMessage1 = new Message(Message.MessageTypes.PAIRING);
                                pairingMessage1.content = client.getPlayerName(); // Send opponent's name
                                Server.SendMessage(this.client, pairingMessage1);
                                
                                Message pairingMessage2 = new Message(Message.MessageTypes.PAIRING);
                                pairingMessage2.content = this.client.getPlayerName(); // Send opponent's name
                                Server.SendMessage(chosenPair, pairingMessage2);
                                
                                // Handle load game state if applicable
                                if (bothWantToLoad) {
                                    // Both clients want to load games - use the first client's game state
                                    Messages.GameState gameState = this.client.getLoadedGameState();
                                    if (gameState != null) {
                                        // Send the loaded game state to both clients
                                        Message loadGameMsg = new Message(Message.MessageTypes.LOAD_GAME);
                                        loadGameMsg.content = gameState;
                                        Server.SendMessage(this.client, loadGameMsg);
                                        Server.SendMessage(chosenPair, loadGameMsg);
                                        
                                        System.out.println("Load game pairing: " + this.client.getPlayerName() + 
                                                         " and " + client.getPlayerName() + 
                                                         " loading game: " + gameState.getSaveName());
                                    }
                                }
                                
                                //after succeeded pairing, determine the team of the clients which starter for the chess game(black or white)
                                // FIXED: Each client gets their own name as playerName, opponent as opponentName
                                Message clientStartMessage = new Message(Message.MessageTypes.START);
                                clientStartMessage.content = new StartInfo(Team.WHITE, UserRegistry.getDisplayName(this.client.getPlayerName()), UserRegistry.getDisplayName(chosenPair.getPlayerName()));
                                Message pairClientStartMessage = new Message(Message.MessageTypes.START);
                                pairClientStartMessage.content = new StartInfo(Team.BLACK, UserRegistry.getDisplayName(chosenPair.getPlayerName()), UserRegistry.getDisplayName(this.client.getPlayerName()));
                                Server.SendMessage(this.client, clientStartMessage);
                                Server.SendMessage(chosenPair, pairClientStartMessage);
                                
                                // Schedule a coordinated timer start 3 seconds after pairing
                                // Skip for loaded games as they will handle their own timer restoration
                                if (!bothWantToLoad) {
                                    final SClient finalChosenPair = chosenPair; // Create final reference for lambda
                                    javax.swing.Timer timerStartScheduler = new javax.swing.Timer(3000, e -> {
                                    long startTime = System.currentTimeMillis() + 2000;
                                        
                                        Message timerStartMessage1 = new Message(Message.MessageTypes.TIMER_START);
                                        timerStartMessage1.content = startTime;
                                        
                                        Message timerStartMessage2 = new Message(Message.MessageTypes.TIMER_START);
                                        timerStartMessage2.content = startTime;
                                        
                                        Server.SendMessage(this.client, timerStartMessage1);
                                        Server.SendMessage(finalChosenPair, timerStartMessage2);
                                        
                                        System.out.println("DEBUG: Coordinated timer start messages sent to both players");
                                        ((javax.swing.Timer) e.getSource()).stop();
                                    });
                                    timerStartScheduler.setRepeats(false);
                                    timerStartScheduler.start();
                                } else {
                                    System.out.println("DEBUG: Skipping coordinated timer start for loaded game - timers will be restored from save state");
                                }
                                
                                System.out.println("Paired: " + this.client.getPlayerName() + " (WHITE) vs " + client.getPlayerName() + " (BLACK)");
                                break;
                            }
                        }
                    }
                    //do not try anytime this operation. Just every second is enough. Do not need to control is there a client want to pair
                    //in any time. Every second is more optimized solution.So sleep 1 second...
                    if (chosenPair == null && (this.client.isWantToPair || this.client.isWantToLoadGame)) {
                        sleep(1000);
                    }
                }
                // after a completed pairing for a sclient. Release the lock to let other clients match with each other.
                Server.pairingLockForTwoPair.release(1);
            } catch (InterruptedException ex) {
                System.out.println("Pairing thread could not been acquired 1 permit. There is an error occured there.");
                Server.pairingLockForTwoPair.release(1);
            }
        }
    }
}
