package chess_game.Utilities;

import Messages.GameState;
import server.UserRegistry;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * Manages saving and loading of chess game states to/from local files
 */
public class GameStateManager {
    
    private static final String SAVE_DIR = System.getProperty("user.home") + File.separator + ".chess_saves";
    private static final String SAVE_EXTENSION = ".chess";
    
    static {
        // Create save directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(SAVE_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create save directory: " + e.getMessage());
        }
    }
    
    /**
     * Save a game state to file
     * @param gameState The game state to save
     * @return true if saved successfully, false otherwise
     */
    public static boolean saveGame(GameState gameState) {
        if (gameState == null || gameState.getSaveName() == null) {
            return false;
        }
        
        try {
            // Use new naming format: Game_name<>Player1_name(color)<>Player2_name(color)<>Timer1<>Timer2
            // Handle cases where color information might be null (for backward compatibility)
            String player1Color = gameState.getPlayer1Color() != null ? gameState.getPlayer1Color().toString() : "Unknown";
            String player2Color = gameState.getPlayer2Color() != null ? gameState.getPlayer2Color().toString() : "Unknown";
            
            String player1Info = gameState.getPlayer1Name() + "(" + player1Color + ")";
            String player2Info = gameState.getPlayer2Name() + "(" + player2Color + ")";
            String timerInfo = gameState.getWhiteTimeRemaining() + "<>" + gameState.getBlackTimeRemaining();
            
            String uniqueSaveName = String.format("%s<>%s<>%s<>%s",
                gameState.getSaveName(), player1Info, player2Info, timerInfo);
            String filename = sanitizeFilename(uniqueSaveName) + SAVE_EXTENSION;
            String filepath = SAVE_DIR + File.separator + filename;
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath))) {
                oos.writeObject(gameState);
                System.out.println("Game saved successfully to: " + filepath);
                // Timer data saved successfully
                return true;
            }
        } catch (IOException e) {
            System.err.println("Failed to save game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Load a game state from file
     * @param saveName The name of the save to load
     * @return The loaded game state, or null if loading failed
     */
    public static GameState loadGame(String saveName) {
        if (saveName == null || saveName.trim().isEmpty()) {
                            // Load failed - saveName is null or empty
            return null;
        }
        
        // Removed debug output for better performance
        
        try {
            // First, try to find a file that matches exactly
            String filename = sanitizeFilename(saveName) + SAVE_EXTENSION;
            String filepath = SAVE_DIR + File.separator + filename;
            
            // Looking for exact match file
            
            if (Files.exists(Paths.get(filepath))) {
                // Found exact match file, loading...
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath))) {
                    GameState gameState = (GameState) ois.readObject();
                    
                    // Ensure backward compatibility for saves without color information
                    if (gameState.getPlayer1Color() == null && gameState.isAIGame()) {
                        // This is an old AI save - try to determine colors from opponent name
                        if ("AI (White)".equals(gameState.getPlayer2Name())) {
                            gameState.setPlayer1Color(chess_game.Pieces.Team.BLACK);
                            gameState.setPlayer2Color(chess_game.Pieces.Team.WHITE);
                        } else {
                            // Default or "AI (Black)" case
                            gameState.setPlayer1Color(chess_game.Pieces.Team.WHITE);
                            gameState.setPlayer2Color(chess_game.Pieces.Team.BLACK);
                        }
                        // Updated old save with color information
                    }
                    
                    System.out.println("Game loaded successfully from: " + filepath);
                    // Timer data loaded successfully
                    return gameState;
                } catch (Exception e) {
                    // Failed to load exact match file, will try pattern matching
                }
            } else {
                // Exact match file not found, searching with pattern
            }
            
            // If not found, search for files that contain the save name
            // Searching for files containing saveName
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(SAVE_DIR), "*" + SAVE_EXTENSION)) {
                for (Path path : stream) {
                    String pathFilename = path.getFileName().toString();
                    // Checking file
                    // First check if filename contains the save name
                    if (pathFilename.contains(saveName)) {
                        // Found matching file, loading...
                        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path.toFile()))) {
                            GameState gameState = (GameState) ois.readObject();
                            
                            // Ensure backward compatibility for saves without color information
                            if (gameState.getPlayer1Color() == null && gameState.isAIGame()) {
                                // This is an old AI save - try to determine colors from opponent name
                                if ("AI (White)".equals(gameState.getPlayer2Name())) {
                                    gameState.setPlayer1Color(chess_game.Pieces.Team.BLACK);
                                    gameState.setPlayer2Color(chess_game.Pieces.Team.WHITE);
                                } else {
                                    // Default or "AI (Black)" case
                                    gameState.setPlayer1Color(chess_game.Pieces.Team.WHITE);
                                    gameState.setPlayer2Color(chess_game.Pieces.Team.BLACK);
                                }
                                // Updated old save with color information
                            }
                            
                            if (gameState.getSaveName().equals(saveName)) {
                                System.out.println("Game loaded successfully from: " + path.toString());
                                // Timer data loaded successfully
                                return gameState;
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            // Log the specific error for debugging
                            System.err.println("Failed to load save file " + path.getFileName() + ": " + e.toString());
                            // Skip this file and continue searching
                            continue;
                        }
                    }
                }
            }
            
            System.err.println("Save file not found for save name: " + saveName);
            System.err.println("Searched in directory: " + SAVE_DIR);
            return null;
            
        } catch (IOException e) {
            System.err.println("Failed to load game: " + e.toString());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get a list of all saved games
     * @return List of GameState objects for all saved games
     */
    public static List<GameState> getAllSavedGames() {
        List<GameState> savedGames = new ArrayList<>();
        
        try {
            if (!Files.exists(Paths.get(SAVE_DIR))) {
                return savedGames;
            }
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(SAVE_DIR), "*" + SAVE_EXTENSION)) {
                for (Path path : stream) {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path.toFile()))) {
                        GameState gameState = (GameState) ois.readObject();
                        
                        // Ensure backward compatibility for saves without color information
                        if (gameState.getPlayer1Color() == null && gameState.isAIGame()) {
                            // This is an old AI save - try to determine colors from opponent name
                            if ("AI (White)".equals(gameState.getPlayer2Name())) {
                                gameState.setPlayer1Color(chess_game.Pieces.Team.BLACK);
                                gameState.setPlayer2Color(chess_game.Pieces.Team.WHITE);
                            } else {
                                // Default or "AI (Black)" case
                                gameState.setPlayer1Color(chess_game.Pieces.Team.WHITE);
                                gameState.setPlayer2Color(chess_game.Pieces.Team.BLACK);
                            }
                            // Updated old save with color information
                        }
                        
                        savedGames.add(gameState);
                    } catch (IOException | ClassNotFoundException e) {
                        // Skip corrupted save file and continue with others
                    }
                }
            }
            
            // Sort by save time (newest first)
            savedGames.sort((a, b) -> Long.compare(b.getSaveTime(), a.getSaveTime()));
            
        } catch (IOException e) {
            System.err.println("Failed to list saved games: " + e.getMessage());
        }
        
        return savedGames;
    }
    
    /**
     * Get a list of all saved games accessible by a specific player
     * @param playerUniqueUsername The player's unique username
     * @return List of GameState objects accessible by the player
     */
    public static List<GameState> getPlayerAccessibleGames(String playerUniqueUsername) {
        List<GameState> allGames = getAllSavedGames();
        List<GameState> accessibleGames = new ArrayList<>();
        
        for (GameState gameState : allGames) {
            if (UserRegistry.canAccessGame(playerUniqueUsername, gameState.getPlayer1Name()) ||
                UserRegistry.canAccessGame(playerUniqueUsername, gameState.getPlayer2Name())) {
                accessibleGames.add(gameState);
            }
        }
        
        return accessibleGames;
    }
    
    /**
     * Get a list of save names only (for display in lists)
     * @return List of save names
     */
    public static List<String> getSaveNames() {
        List<String> saveNames = new ArrayList<>();
        
        try {
            if (!Files.exists(Paths.get(SAVE_DIR))) {
                return saveNames;
            }
            
            // Get file information without loading full game states
            List<SaveFileInfo> saveFileInfos = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(SAVE_DIR), "*" + SAVE_EXTENSION)) {
                for (Path path : stream) {
                    try {
                        // Only read the basic info we need for display
                        String displayName = getDisplayNameFromFile(path.toFile());
                        if (displayName != null) {
                            long lastModified = Files.getLastModifiedTime(path).toMillis();
                            saveFileInfos.add(new SaveFileInfo(displayName, lastModified));
                        }
                    } catch (Exception e) {
                        // Skip corrupted files
                        continue;
                    }
                }
            }
            
            // Sort by last modified time (newest first)
            saveFileInfos.sort((a, b) -> Long.compare(b.lastModified, a.lastModified));
            
            // Extract just the display names
            for (SaveFileInfo info : saveFileInfos) {
                saveNames.add(info.displayName);
            }
            
        } catch (IOException e) {
            System.err.println("Failed to list saved games: " + e.getMessage());
        }
        
        return saveNames;
    }
    
    /**
     * Helper class to store basic save file information
     */
    private static class SaveFileInfo {
        final String displayName;
        final long lastModified;
        
        SaveFileInfo(String displayName, long lastModified) {
            this.displayName = displayName;
            this.lastModified = lastModified;
        }
    }
    
    /**
     * Get display name from save file without fully deserializing
     * @param file The save file
     * @return Display name or null if file cannot be read
     */
    private static String getDisplayNameFromFile(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            GameState gameState = (GameState) ois.readObject();
            
            // Basic display info - avoid heavy processing
            String saveName = gameState.getSaveName();
            String player1 = gameState.getPlayer1Name();
            String player2 = gameState.getPlayer2Name();
            
            // Quick format without color processing
            if (gameState.isAIGame()) {
                return saveName + " (" + player1 + " vs AI) [AI Game] - " + formatTimestamp(gameState.getSaveTime());
            } else {
                return saveName + " (" + player1 + " vs " + player2 + ") [Multiplayer] - " + formatTimestamp(gameState.getSaveTime());
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get a list of save names accessible by a specific player (for display in lists)
     * @param playerUniqueUsername The player's unique username
     * @return List of save names accessible by the player
     */
    public static List<String> getPlayerSaveNames(String playerUniqueUsername) {
        List<String> saveNames = new ArrayList<>();
        
        try {
            if (!Files.exists(Paths.get(SAVE_DIR))) {
                return saveNames;
            }
            
            // Get file information without loading full game states
            List<SaveFileInfo> saveFileInfos = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(SAVE_DIR), "*" + SAVE_EXTENSION)) {
                for (Path path : stream) {
                    try {
                        // Quick check if player can access this game
                        if (canPlayerAccessFile(playerUniqueUsername, path.toFile())) {
                            String displayName = getDisplayNameFromFile(path.toFile());
                            if (displayName != null) {
                                long lastModified = Files.getLastModifiedTime(path).toMillis();
                                saveFileInfos.add(new SaveFileInfo(displayName, lastModified));
                            }
                        }
                    } catch (Exception e) {
                        // Skip corrupted files
                        continue;
                    }
                }
            }
            
            // Sort by last modified time (newest first)
            saveFileInfos.sort((a, b) -> Long.compare(b.lastModified, a.lastModified));
            
            // Extract just the display names
            for (SaveFileInfo info : saveFileInfos) {
                saveNames.add(info.displayName);
            }
            
        } catch (IOException e) {
            System.err.println("Failed to list saved games: " + e.getMessage());
        }
        
        return saveNames;
    }
    
    /**
     * Quick check if player can access a save file without full deserialization
     * @param playerUniqueUsername The player's unique username
     * @param file The save file
     * @return true if player can access, false otherwise
     */
    private static boolean canPlayerAccessFile(String playerUniqueUsername, File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            GameState gameState = (GameState) ois.readObject();
            
            return UserRegistry.canAccessGame(playerUniqueUsername, gameState.getPlayer1Name()) ||
                   UserRegistry.canAccessGame(playerUniqueUsername, gameState.getPlayer2Name());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Delete a saved game
     * @param saveName The name of the save to delete
     * @return true if deleted successfully, false otherwise
     */
    public static boolean deleteSave(String saveName) {
        if (saveName == null || saveName.trim().isEmpty()) {
            return false;
        }
        
        try {
            String filename = sanitizeFilename(saveName) + SAVE_EXTENSION;
            String filepath = SAVE_DIR + File.separator + filename;
            
            boolean deleted = Files.deleteIfExists(Paths.get(filepath));
            if (!deleted) {
                System.err.println("File not found or could not be deleted: " + filepath);
            }
            return deleted;
        } catch (IOException e) {
            System.err.println("Failed to delete save: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if a save with the given name exists
     * @param saveName The name to check
     * @return true if save exists, false otherwise
     */
    public static boolean saveExists(String saveName) {
        if (saveName == null || saveName.trim().isEmpty()) {
            return false;
        }
        
        String filename = sanitizeFilename(saveName) + SAVE_EXTENSION;
        String filepath = SAVE_DIR + File.separator + filename;
        
        return Files.exists(Paths.get(filepath));
    }
    
    /**
     * Sanitize filename to remove invalid characters
     * @param filename The filename to sanitize
     * @return Sanitized filename
     */
    private static String sanitizeFilename(String filename) {
        // Remove invalid characters for filenames
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }
    
    /**
     * Format timestamp for display
     * @param timestamp The timestamp to format
     * @return Formatted timestamp string
     */
    private static String formatTimestamp(long timestamp) {
        return new java.text.SimpleDateFormat("MMM dd, HH:mm").format(new java.util.Date(timestamp));
    }
    
    /**
     * Get the save directory path
     * @return The save directory path
     */
    public static String getSaveDirectory() {
        return SAVE_DIR;
    }
}
