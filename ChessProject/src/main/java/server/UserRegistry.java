package server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.io.Serializable;

/**
 * UserRegistry manages unique usernames by preventing duplicates.
 * Each username must be unique - no numbers are added automatically.
 * If a username is already taken, the user must choose a different one.
 */
public class UserRegistry implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Thread-safe storage for active usernames
    private static final Set<String> activeUsernames = ConcurrentHashMap.newKeySet();
    
    // Track usernames that are currently in use (for session management)
    private static final ConcurrentHashMap<String, Long> usernameTimestamps = new ConcurrentHashMap<>();
    
    /**
     * Attempts to register a username. Returns the username if successful, or null if taken.
     * 
     * @param username The desired username
     * @return The username if successful, null if already taken
     */
    public static synchronized String registerUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        
        username = username.trim();
        
        // Check if username is already taken
        if (activeUsernames.contains(username)) {
            System.out.println("UserRegistry: Username '" + username + "' is already taken");
            return null;
        }
        
        // Register the username
        activeUsernames.add(username);
        usernameTimestamps.put(username, System.currentTimeMillis());
        
        System.out.println("UserRegistry: Successfully registered username '" + username + "'");
        return username;
    }
    
    /**
     * Checks if a username is currently registered.
     * 
     * @param username The username to check
     * @return true if the username is registered, false otherwise
     */
    public static boolean isUsernameRegistered(String username) {
        return activeUsernames.contains(username);
    }
    
    /**
     * Unregisters a username (when user disconnects).
     * 
     * @param username The username to unregister
     * @return true if successfully unregistered, false if not found
     */
    public static synchronized boolean unregisterUsername(String username) {
        if (username == null) {
            return false;
        }
        
        boolean removed = activeUsernames.remove(username);
        usernameTimestamps.remove(username);
        
        if (removed) {
            System.out.println("UserRegistry: Unregistered username '" + username + "'");
        }
        
        return removed;
    }
    
    /**
     * Checks if a player can access a saved game based on their username.
     * Now with simplified logic - exact match required.
     * 
     * @param playerUsername The player's username
     * @param gamePlayerName The player name stored in the game
     * @return true if the player can access the game, false otherwise
     */
    public static boolean canAccessGame(String playerUsername, String gamePlayerName) {
        if (playerUsername == null || gamePlayerName == null) {
            return false;
        }
        
        // Direct match
        return playerUsername.equals(gamePlayerName);
    }
    
    /**
     * Gets the display name from a username (same as input now).
     * 
     * @param username The username
     * @return The display name (same as username)
     */
    public static String getDisplayName(String username) {
        return username;
    }
    
    /**
     * Gets the current number of registered users.
     * 
     * @return The number of registered usernames
     */
    public static int getRegisteredUserCount() {
        return activeUsernames.size();
    }
    
    /**
     * Gets all currently registered usernames.
     * 
     * @return Set of active usernames
     */
    public static Set<String> getActiveUsernames() {
        return new java.util.HashSet<>(activeUsernames);
    }
    
    /**
     * Clears all registered usernames (for testing purposes).
     * WARNING: This will reset all user registrations!
     */
    public static synchronized void clearRegistry() {
        activeUsernames.clear();
        usernameTimestamps.clear();
        System.out.println("UserRegistry: Registry cleared");
    }
    
    /**
     * Debug method to print current registry state.
     */
    public static void printRegistryState() {
        System.out.println("=== UserRegistry State ===");
        System.out.println("Registered users: " + getRegisteredUserCount());
        System.out.println("Active usernames:");
        activeUsernames.forEach(username -> 
            System.out.println("  " + username + " (since " + usernameTimestamps.get(username) + ")"));
        System.out.println("========================");
    }
}
