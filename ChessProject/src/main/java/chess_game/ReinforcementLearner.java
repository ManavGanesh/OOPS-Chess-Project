package chess_game;

import chess_game.Pieces.Team;
import chess_game.GameData.GameOutcome;
import chess_game.GameData.GameState;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.concurrent.*;

/**
 * Reinforcement learning model for improving AI based on gameplay
 */
public class ReinforcementLearner {
    private Queue<GameData> gameQueue;
    private Map<String, Double> qValues;
    private Map<String, Integer> moveFrequency;
    private Map<String, Double> tacticalPatterns;
    private Map<String, Double> positionalPatterns;
    private final double learningRate = 0.1;
    private final double discountFactor = 0.9;
    private final String qValuesFile = "chess_learning_data.ser";
    private int gamesPlayed = 0;
    private int gamesWon = 0;
    private int gamesLost = 0;
    private int gamesDraw = 0;
    private ExecutorService executor;
    private final int maxQueueSize = 100;

    public ReinforcementLearner() {
        this.gameQueue = new LinkedList<>();
        this.executor = Executors.newFixedThreadPool(4);
        this.qValues = new HashMap<>();
        this.moveFrequency = new HashMap<>();
        this.tacticalPatterns = new HashMap<>();
        this.positionalPatterns = new HashMap<>();
        loadLearningData();
    }

    /**
     * Add a completed game to the learning queue
     */
    public void queueGame(GameData gameData) {
        synchronized (gameQueue) {
            if (gameQueue.size() >= maxQueueSize) {
                gameQueue.poll(); // Remove oldest game
            }
            gameQueue.add(gameData);
            processQueue();
        }
    }

    /**
     * Process queued games for learning
     */
    private void processQueue() {
        executor.submit(() -> {
            synchronized (gameQueue) {
                while (!gameQueue.isEmpty()) {
                    GameData gameData = gameQueue.poll();
                    if (gameData != null) {
                        learnFromGame(gameData);
                    }
                }
            }
        });
    }

    /**
     * Reinforcement learning logic on game data
     */
    private void learnFromGame(GameData gameData) {
        List<GameState> states = gameData.getGameStates();
        GameOutcome outcome = gameData.getOutcome();

        // Iterate over each state and simulate learning process
        for (GameState state : states) {
            adjustStrategy(state, outcome, gameData);
        }
        updateGameStatistics(gameData.getOutcome());
        learnTacticalPatterns(gameData);
        learnPositionalPatterns(gameData);
        saveLearningData();
    }

    /**
     * Q-learning update for strategies with temporal difference learning
     */
    private void updateQValue(String state, double reward) {
        double currentQ = qValues.getOrDefault(state, 0.0);
        double newQ = currentQ + learningRate * (reward - currentQ);
        qValues.put(state, newQ);
        
        // Update move frequency for exploration vs exploitation
        moveFrequency.put(state, moveFrequency.getOrDefault(state, 0) + 1);
    }
    
    /**
     * Get Q-value for a state-action pair
     */
    public double getQValue(String state) {
        return qValues.getOrDefault(state, 0.0);
    }
    
    /**
     * Get exploration bonus based on move frequency
     */
    public double getExplorationBonus(String state) {
        int frequency = moveFrequency.getOrDefault(state, 0);
        // UCB1 exploration bonus
        return frequency > 0 ? Math.sqrt(2 * Math.log(gamesPlayed + 1) / frequency) : 1.0;
    }

    /**
     * Adjusts AI strategy based on game outcome and state
     */
    private void adjustStrategy(GameState state, GameOutcome outcome, GameData gameData) {
        // Placeholder for reinforcement learning adjustment logic
        // Use state and outcome to update weights, biases, etc.
        String stateHash = state.getBoardHash() + state.getMove().getNotation();
        double reward = 0;
        switch (outcome) {
            case WHITE_WINS:
                reward = state.getPlayerTeam() == Team.WHITE ? 1 : -1;
                break;
            case BLACK_WINS:
                reward = state.getPlayerTeam() == Team.BLACK ? 1 : -1;
                break;
            case DRAW:
                reward = 0.5;
                break;
            default:
                reward = 0;
                break;
        }
        // Apply temporal difference learning with decay for older moves
        double timeDecay = Math.pow(0.95, gameData.getGameStates().size() - gameData.getGameStates().indexOf(state));
        updateQValue(stateHash, reward * timeDecay);
        
        // Learn from tactical patterns
        if (state.wasCapture() || state.wasCheck() || state.wasCheckmate()) {
            String tacticalPattern = extractTacticalPattern(state);
            double tacticalReward = reward * (state.wasCheckmate() ? 2.0 : state.wasCheck() ? 1.5 : 1.2);
            tacticalPatterns.put(tacticalPattern, 
                tacticalPatterns.getOrDefault(tacticalPattern, 0.0) + learningRate * tacticalReward);
        }
    }

    /**
     * Extract tactical pattern from game state
     */
    private String extractTacticalPattern(GameState state) {
        StringBuilder pattern = new StringBuilder();
        
        if (state.wasCapture()) {
            pattern.append("CAPTURE_");
        }
        if (state.wasCheck()) {
            pattern.append("CHECK_");
        }
        if (state.wasCheckmate()) {
            pattern.append("CHECKMATE_");
        }
        
        // Add piece type that made the move
        String moveNotation = state.getMove().getNotation();
        if (moveNotation.length() > 0) {
            pattern.append(moveNotation.charAt(0)).append("_");
        }
        
        return pattern.toString();
    }
    
    /**
     * Learn tactical patterns from completed games
     */
    private void learnTacticalPatterns(GameData gameData) {
        List<GameState> states = gameData.getGameStates();
        GameOutcome outcome = gameData.getOutcome();
        
        for (int i = 0; i < states.size() - 1; i++) {
            GameState currentState = states.get(i);
            GameState nextState = states.get(i + 1);
            
            // Look for tactical sequences
            if (currentState.wasCapture() && nextState.wasCheck()) {
                String pattern = "CAPTURE_TO_CHECK";
                double reward = outcome == GameOutcome.WHITE_WINS || outcome == GameOutcome.BLACK_WINS ? 1.0 : 0.5;
                tacticalPatterns.put(pattern, 
                    tacticalPatterns.getOrDefault(pattern, 0.0) + learningRate * reward);
            }
            
            // Learn fork patterns
            if (isLikelyFork(currentState)) {
                String pattern = "FORK_" + currentState.getMove().getNotation().charAt(0);
                double reward = outcome == GameOutcome.WHITE_WINS || outcome == GameOutcome.BLACK_WINS ? 1.5 : 0.5;
                tacticalPatterns.put(pattern, 
                    tacticalPatterns.getOrDefault(pattern, 0.0) + learningRate * reward);
            }
        }
    }
    
    /**
     * Learn positional patterns
     */
    private void learnPositionalPatterns(GameData gameData) {
        List<GameState> states = gameData.getGameStates();
        GameOutcome outcome = gameData.getOutcome();
        
        for (GameState state : states) {
            String positionalKey = extractPositionalPattern(state);
            double reward = getOutcomeReward(outcome, state.getPlayerTeam());
            
            positionalPatterns.put(positionalKey,
                positionalPatterns.getOrDefault(positionalKey, 0.0) + learningRate * reward * 0.1);
        }
    }
    
    /**
     * Extract positional pattern from game state
     */
    private String extractPositionalPattern(GameState state) {
        StringBuilder pattern = new StringBuilder();
        
        String moveNotation = state.getMove().getNotation();
        if (moveNotation.length() >= 4) {
            // Extract destination square type (center, edge, corner)
            char destX = moveNotation.charAt(moveNotation.length() - 2);
            char destY = moveNotation.charAt(moveNotation.length() - 1);
            
            if ((destX == '3' || destX == '4') && (destY == '3' || destY == '4')) {
                pattern.append("CENTER_");
            } else if (destX == '0' || destX == '7' || destY == '0' || destY == '7') {
                pattern.append("EDGE_");
            }
            
            pattern.append(moveNotation.charAt(0)); // Piece type
        }
        
        return pattern.toString();
    }
    
    /**
     * Check if a move is likely a fork
     */
    private boolean isLikelyFork(GameState state) {
        // This is a simplified heuristic - in a real implementation,
        // you'd analyze the board position more thoroughly
        return state.wasCapture() && state.getEvaluationScore() > 5.0;
    }
    
    /**
     * Get reward based on game outcome
     */
    private double getOutcomeReward(GameOutcome outcome, Team playerTeam) {
        switch (outcome) {
            case WHITE_WINS:
                return playerTeam == Team.WHITE ? 1.0 : -1.0;
            case BLACK_WINS:
                return playerTeam == Team.BLACK ? 1.0 : -1.0;
            case DRAW:
            case STALEMATE:
                return 0.5;
            default:
                return 0.0;
        }
    }
    
    /**
     * Update game statistics
     */
    private void updateGameStatistics(GameOutcome outcome) {
        gamesPlayed++;
        switch (outcome) {
            case WHITE_WINS:
            case BLACK_WINS:
                if (outcome == GameOutcome.WHITE_WINS) gamesWon++; else gamesLost++;
                break;
            case DRAW:
            case STALEMATE:
                gamesDraw++;
                break;
        }
    }
    
    /**
     * Get tactical pattern value
     */
    public double getTacticalPatternValue(String pattern) {
        return tacticalPatterns.getOrDefault(pattern, 0.0);
    }
    
    /**
     * Get positional pattern value
     */
    public double getPositionalPatternValue(String pattern) {
        return positionalPatterns.getOrDefault(pattern, 0.0);
    }
    
    /**
     * Load all learning data from file
     */
    private void loadLearningData() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(qValuesFile))) {
            LearningData data = (LearningData) ois.readObject();
            this.qValues = data.qValues;
            this.moveFrequency = data.moveFrequency;
            this.tacticalPatterns = data.tacticalPatterns;
            this.positionalPatterns = data.positionalPatterns;
            this.gamesPlayed = data.gamesPlayed;
            this.gamesWon = data.gamesWon;
            this.gamesLost = data.gamesLost;
            this.gamesDraw = data.gamesDraw;
            System.out.println("Loaded learning data: " + gamesPlayed + " games played");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Learning data file not found, starting fresh.");
        }
    }
    
    /**
     * Save all learning data to file
     */
    private void saveLearningData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(qValuesFile))) {
            LearningData data = new LearningData(
                qValues, moveFrequency, tacticalPatterns, positionalPatterns,
                gamesPlayed, gamesWon, gamesLost, gamesDraw
            );
            oos.writeObject(data);
            System.out.println("Saved learning data: " + gamesPlayed + " games total");
        } catch (IOException e) {
            System.out.println("Failed to save learning data: " + e.getMessage());
        }
    }
    
    /**
     * Get learning statistics
     */
    public String getLearningStats() {
        double winRate = gamesPlayed > 0 ? (double) gamesWon / gamesPlayed * 100 : 0;
        return String.format("Games: %d, Win Rate: %.1f%%, Q-Values: %d, Tactical Patterns: %d", 
            gamesPlayed, winRate, qValues.size(), tacticalPatterns.size());
    }
    
    /**
     * Data class for serialization
     */
    private static class LearningData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        final Map<String, Double> qValues;
        final Map<String, Integer> moveFrequency;
        final Map<String, Double> tacticalPatterns;
        final Map<String, Double> positionalPatterns;
        final int gamesPlayed;
        final int gamesWon;
        final int gamesLost;
        final int gamesDraw;
        
        LearningData(Map<String, Double> qValues, Map<String, Integer> moveFrequency,
                    Map<String, Double> tacticalPatterns, Map<String, Double> positionalPatterns,
                    int gamesPlayed, int gamesWon, int gamesLost, int gamesDraw) {
            this.qValues = qValues;
            this.moveFrequency = moveFrequency;
            this.tacticalPatterns = tacticalPatterns;
            this.positionalPatterns = positionalPatterns;
            this.gamesPlayed = gamesPlayed;
            this.gamesWon = gamesWon;
            this.gamesLost = gamesLost;
            this.gamesDraw = gamesDraw;
        }
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
