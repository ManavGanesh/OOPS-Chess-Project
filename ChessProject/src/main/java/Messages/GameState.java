package Messages;

import chess_game.Boards.Board;
import chess_game.Boards.Tile;
import chess_game.Pieces.Team;
import java.io.Serializable;

/**
 * Represents a saved game state that can be serialized and transmitted
 * between client and server for save/load functionality.
 */
public class GameState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Board board;
    private String player1Name;
    private String player2Name;
    private Team currentPlayerTeam;
    private String saveName;
    private long saveTime;
    private boolean isAIGame; // New field to track if this was originally an AI game
    
    // Player color information for multiplayer games
    private Team player1Color;
    private Team player2Color;
    
    // Timer state information for multiplayer games
    private int whiteTimeRemaining; // in seconds
    private int blackTimeRemaining; // in seconds
    private boolean timersWereActive; // whether timers were running when saved
    private boolean wasWhiteTurn; // whose turn it was when saved

    public GameState(Board board, String player1Name, String player2Name, 
                    Team currentPlayerTeam, String saveName, boolean isAIGame) {
        this.board = board;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.currentPlayerTeam = currentPlayerTeam;
        this.saveName = saveName;
        this.saveTime = System.currentTimeMillis();
        this.isAIGame = isAIGame;
        
        // Board state saved
        
        // For AI games, colors will be set explicitly by the caller
        // For multiplayer games, colors will be set when saving
        this.player1Color = null;
        this.player2Color = null;
        
        // Initialize timer values to default (10 minutes = 600 seconds)
        this.whiteTimeRemaining = 600;
        this.blackTimeRemaining = 600;
        this.timersWereActive = false;
        this.wasWhiteTurn = true;
    }

    private void debugPrintBoardState(String context) {
        System.out.println("DEBUG: " + context + " - Board State");
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Tile tile = board.getTile(i, j);
                if (tile.hasPiece()) {
                    System.out.print(tile.getPiece().getType() + " " + tile.getPiece().getTeam() + " ");
                } else {
                    System.out.print("Empty ");
                }
            }
            System.out.println();
        }
    }
    
    // Constructor with player colors for multiplayer games
    public GameState(Board board, String player1Name, String player2Name, 
                    Team currentPlayerTeam, String saveName, boolean isAIGame,
                    Team player1Color, Team player2Color) {
        this(board, player1Name, player2Name, currentPlayerTeam, saveName, isAIGame);
        this.player1Color = player1Color;
        this.player2Color = player2Color;
    }
    
    // Constructor with timer state for multiplayer games
    public GameState(Board board, String player1Name, String player2Name, 
                    Team currentPlayerTeam, String saveName, boolean isAIGame,
                    Team player1Color, Team player2Color,
                    int whiteTimeRemaining, int blackTimeRemaining,
                    boolean timersWereActive, boolean wasWhiteTurn) {
        this(board, player1Name, player2Name, currentPlayerTeam, saveName, isAIGame, player1Color, player2Color);
        this.whiteTimeRemaining = whiteTimeRemaining;
        this.blackTimeRemaining = blackTimeRemaining;
        this.timersWereActive = timersWereActive;
        this.wasWhiteTurn = wasWhiteTurn;
    }
    
    // Backward compatibility constructor
    public GameState(Board board, String player1Name, String player2Name, 
                    Team currentPlayerTeam, String saveName) {
        this(board, player1Name, player2Name, currentPlayerTeam, saveName, 
             "AI".equals(player2Name) || "AI (Black)".equals(player2Name));
    }
    
    // Getters
    public Board getBoard() {
        // Board state loaded
        return board;
    }
    
    public String getPlayer1Name() {
        return player1Name;
    }
    
    public String getPlayer2Name() {
        return player2Name;
    }
    
    public Team getCurrentPlayerTeam() {
        return currentPlayerTeam;
    }
    
    public String getSaveName() {
        return saveName;
    }
    
    public long getSaveTime() {
        return saveTime;
    }
    
    public boolean isAIGame() {
        return isAIGame;
    }
    
    public Team getPlayer1Color() {
        return player1Color;
    }
    
    public Team getPlayer2Color() {
        return player2Color;
    }
    
    public int getWhiteTimeRemaining() {
        return whiteTimeRemaining;
    }
    
    public int getBlackTimeRemaining() {
        return blackTimeRemaining;
    }
    
    public boolean getTimersWereActive() {
        return timersWereActive;
    }
    
    public boolean getWasWhiteTurn() {
        return wasWhiteTurn;
    }
    
    // Setters
    public void setBoard(Board board) {
        this.board = board;
    }
    
    public void setPlayer1Name(String player1Name) {
        this.player1Name = player1Name;
    }
    
    public void setPlayer2Name(String player2Name) {
        this.player2Name = player2Name;
    }
    
    public void setCurrentPlayerTeam(Team currentPlayerTeam) {
        this.currentPlayerTeam = currentPlayerTeam;
    }
    
    public void setSaveName(String saveName) {
        this.saveName = saveName;
    }
    
    public void setSaveTime(long saveTime) {
        this.saveTime = saveTime;
    }
    
    public void setAIGame(boolean isAIGame) {
        this.isAIGame = isAIGame;
    }
    
    public void setPlayer1Color(Team player1Color) {
        this.player1Color = player1Color;
    }
    
    public void setPlayer2Color(Team player2Color) {
        this.player2Color = player2Color;
    }
    
    public void setWhiteTimeRemaining(int whiteTimeRemaining) {
        this.whiteTimeRemaining = whiteTimeRemaining;
    }
    
    public void setBlackTimeRemaining(int blackTimeRemaining) {
        this.blackTimeRemaining = blackTimeRemaining;
    }
    
    public void setTimersWereActive(boolean timersWereActive) {
        this.timersWereActive = timersWereActive;
    }
    
    public void setWasWhiteTurn(boolean wasWhiteTurn) {
        this.wasWhiteTurn = wasWhiteTurn;
    }
    
    /**
     * Check if this game state has valid timer data
     * Timer data is valid if this is a multiplayer game (AI games don't use timers)
     */
    public boolean hasTimerData() {
        // Timer data is valid for multiplayer games only
        return !isAIGame;
    }
    
    @Override
    public String toString() {
        String gameType = isAIGame ? "AI" : "Multiplayer";
        String colorInfo = "";
        String timerInfo = "";
        
        if (!isAIGame && player1Color != null && player2Color != null) {
            colorInfo = " [" + player1Name + "(" + player1Color + ") vs " + player2Name + "(" + player2Color + ")]";
            
            // Add timer info for multiplayer games
            int whiteMin = whiteTimeRemaining / 60;
            int whiteSec = whiteTimeRemaining % 60;
            int blackMin = blackTimeRemaining / 60;
            int blackSec = blackTimeRemaining % 60;
            timerInfo = " [Timers: W" + whiteMin + ":" + String.format("%02d", whiteSec) + 
                       " B" + blackMin + ":" + String.format("%02d", blackSec) + "]";
        }
        
        return saveName + " (" + player1Name + " vs " + player2Name + ")" + colorInfo + timerInfo + " [" + gameType + "] - " + 
               new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(saveTime));
    }
} 