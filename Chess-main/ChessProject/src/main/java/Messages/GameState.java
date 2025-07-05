package Messages;

import chess_game.Boards.Board;
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
    
    public GameState(Board board, String player1Name, String player2Name, 
                    Team currentPlayerTeam, String saveName) {
        this.board = board;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.currentPlayerTeam = currentPlayerTeam;
        this.saveName = saveName;
        this.saveTime = System.currentTimeMillis();
    }
    
    // Getters
    public Board getBoard() {
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
    
    @Override
    public String toString() {
        return saveName + " (" + player1Name + " vs " + player2Name + ") - " + 
               new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(saveTime));
    }
} 