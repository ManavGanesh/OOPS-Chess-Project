package chess_game;

import chess_game.Move.Move;
import chess_game.Pieces.Team;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

/**
 * Represents a single chess game's data for reinforcement learning analysis
 */
public class GameData implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String gameId;
    private final Date gameDate;
    private final List<GameState> gameStates;
    private final GameOutcome outcome;
    private final Team winningTeam;
    private final int totalMoves;
    private final long gameDuration; // in milliseconds
    
    public GameData(String gameId, GameOutcome outcome, Team winningTeam, 
                   List<GameState> gameStates, long gameDuration) {
        this.gameId = gameId;
        this.gameDate = new Date();
        this.gameStates = new ArrayList<>(gameStates);
        this.outcome = outcome;
        this.winningTeam = winningTeam;
        this.totalMoves = gameStates.size();
        this.gameDuration = gameDuration;
    }
    
    // Getters
    public String getGameId() { return gameId; }
    public Date getGameDate() { return gameDate; }
    public List<GameState> getGameStates() { return new ArrayList<>(gameStates); }
    public GameOutcome getOutcome() { return outcome; }
    public Team getWinningTeam() { return winningTeam; }
    public int getTotalMoves() { return totalMoves; }
    public long getGameDuration() { return gameDuration; }
    
    /**
     * Represents a single game state with the move made and board position
     */
    public static class GameState implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String boardHash;
        private final Move move;
        private final Team playerTeam;
        private final double evaluationScore;
        private final int moveNumber;
        private final boolean wasCapture;
        private final boolean wasCheck;
        private final boolean wasCheckmate;
        private final long thinkingTime; // in milliseconds
        
        public GameState(String boardHash, Move move, Team playerTeam, 
                        double evaluationScore, int moveNumber, boolean wasCapture,
                        boolean wasCheck, boolean wasCheckmate, long thinkingTime) {
            this.boardHash = boardHash;
            this.move = move;
            this.playerTeam = playerTeam;
            this.evaluationScore = evaluationScore;
            this.moveNumber = moveNumber;
            this.wasCapture = wasCapture;
            this.wasCheck = wasCheck;
            this.wasCheckmate = wasCheckmate;
            this.thinkingTime = thinkingTime;
        }
        
        // Getters
        public String getBoardHash() { return boardHash; }
        public Move getMove() { return move; }
        public Team getPlayerTeam() { return playerTeam; }
        public double getEvaluationScore() { return evaluationScore; }
        public int getMoveNumber() { return moveNumber; }
        public boolean wasCapture() { return wasCapture; }
        public boolean wasCheck() { return wasCheck; }
        public boolean wasCheckmate() { return wasCheckmate; }
        public long getThinkingTime() { return thinkingTime; }
    }
    
    /**
     * Possible game outcomes
     */
    public enum GameOutcome {
        WHITE_WINS,
        BLACK_WINS,
        DRAW,
        STALEMATE,
        RESIGNATION,
        TIMEOUT
    }
    
    /**
     * Get statistics for this game
     */
    public GameStatistics getStatistics() {
        return new GameStatistics(this);
    }
    
    /**
     * Statistical analysis of the game
     */
    public static class GameStatistics {
        private final int totalMoves;
        private final int captures;
        private final int checks;
        private final double averageEvaluationScore;
        private final long averageThinkingTime;
        private final long totalGameTime;
        
        public GameStatistics(GameData gameData) {
            this.totalMoves = gameData.getTotalMoves();
            this.totalGameTime = gameData.getGameDuration();
            
            int captureCount = 0;
            int checkCount = 0;
            double scoreSum = 0;
            long thinkingTimeSum = 0;
            
            for (GameState state : gameData.getGameStates()) {
                if (state.wasCapture()) captureCount++;
                if (state.wasCheck()) checkCount++;
                scoreSum += state.getEvaluationScore();
                thinkingTimeSum += state.getThinkingTime();
            }
            
            this.captures = captureCount;
            this.checks = checkCount;
            this.averageEvaluationScore = totalMoves > 0 ? scoreSum / totalMoves : 0;
            this.averageThinkingTime = totalMoves > 0 ? thinkingTimeSum / totalMoves : 0;
        }
        
        // Getters
        public int getTotalMoves() { return totalMoves; }
        public int getCaptures() { return captures; }
        public int getChecks() { return checks; }
        public double getAverageEvaluationScore() { return averageEvaluationScore; }
        public long getAverageThinkingTime() { return averageThinkingTime; }
        public long getTotalGameTime() { return totalGameTime; }
    }
}
