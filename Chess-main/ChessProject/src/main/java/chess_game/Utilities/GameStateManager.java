package chess_game.Utilities;

import Messages.GameState;
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
            String filename = sanitizeFilename(gameState.getSaveName()) + SAVE_EXTENSION;
            String filepath = SAVE_DIR + File.separator + filename;
            
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filepath))) {
                oos.writeObject(gameState);
                System.out.println("Game saved successfully to: " + filepath);
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
            return null;
        }
        
        try {
            String filename = sanitizeFilename(saveName) + SAVE_EXTENSION;
            String filepath = SAVE_DIR + File.separator + filename;
            
            if (!Files.exists(Paths.get(filepath))) {
                System.err.println("Save file not found: " + filepath);
                return null;
            }
            
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath))) {
                GameState gameState = (GameState) ois.readObject();
                System.out.println("Game loaded successfully from: " + filepath);
                return gameState;
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load game: " + e.getMessage());
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
                        savedGames.add(gameState);
                    } catch (IOException | ClassNotFoundException e) {
                        System.err.println("Failed to load save file: " + path.getFileName() + " - " + e.getMessage());
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
     * Get a list of save names only (for display in lists)
     * @return List of save names
     */
    public static List<String> getSaveNames() {
        List<String> saveNames = new ArrayList<>();
        List<GameState> savedGames = getAllSavedGames();
        
        for (GameState gameState : savedGames) {
            saveNames.add(gameState.toString());
        }
        
        return saveNames;
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
            
            return Files.deleteIfExists(Paths.get(filepath));
        } catch (IOException e) {
            System.err.println("Failed to delete save: " + e.getMessage());
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
     * Get the save directory path
     * @return The save directory path
     */
    public static String getSaveDirectory() {
        return SAVE_DIR;
    }
}
