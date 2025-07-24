package server;

import chess_game.Utilities.GameStateManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;

/**
 * Utility class to clean up the chess game system including user registry and saved games.
 * This is useful for testing, resetting the system, or maintenance purposes.
 */
public class SystemCleanup {
    
    /**
     * Main method to run the cleanup utility interactively
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("=== Chess System Cleanup Utility ===");
        System.out.println();
        
        // Show current state
        showCurrentState();
        
        System.out.println("What would you like to clean up?");
        System.out.println("1. Clear User Registry only");
        System.out.println("2. Clear Saved Games only");
        System.out.println("3. Clear Both (Complete Reset)");
        System.out.println("4. Show Current State");
        System.out.println("5. Exit");
        System.out.print("Enter your choice (1-5): ");
        
        int choice = scanner.nextInt();
        
        switch (choice) {
            case 1:
                clearUserRegistryInteractive(scanner);
                break;
            case 2:
                clearSavedGamesInteractive(scanner);
                break;
            case 3:
                clearBothInteractive(scanner);
                break;
            case 4:
                showCurrentState();
                break;
            case 5:
                System.out.println("Exiting without changes.");
                break;
            default:
                System.out.println("Invalid choice. Exiting.");
        }
        
        scanner.close();
    }
    
    /**
     * Shows the current state of the system
     */
    public static void showCurrentState() {
        System.out.println("=== Current System State ===");
        
        // User Registry State
        System.out.println("User Registry:");
        System.out.println("  Registered users: " + UserRegistry.getRegisteredUserCount());
        
        // Saved Games State
        String saveDir = GameStateManager.getSaveDirectory();
        File saveDirFile = new File(saveDir);
        
        if (saveDirFile.exists() && saveDirFile.isDirectory()) {
            File[] saveFiles = saveDirFile.listFiles((dir, name) -> name.endsWith(".chess"));
            int saveCount = saveFiles != null ? saveFiles.length : 0;
            System.out.println("Saved Games:");
            System.out.println("  Save directory: " + saveDir);
            System.out.println("  Number of saved games: " + saveCount);
            
            if (saveCount > 0 && saveCount <= 10) {
                System.out.println("  Game files:");
                for (File file : saveFiles) {
                    System.out.println("    - " + file.getName());
                }
            } else if (saveCount > 10) {
                System.out.println("  (Too many files to list - " + saveCount + " total)");
            }
        } else {
            System.out.println("Saved Games:");
            System.out.println("  Save directory: " + saveDir + " (does not exist)");
            System.out.println("  Number of saved games: 0");
        }
        
        System.out.println("========================");
        System.out.println();
    }
    
    /**
     * Clear user registry with confirmation
     */
    private static void clearUserRegistryInteractive(Scanner scanner) {
        System.out.println();
        System.out.println("⚠️  WARNING: This will clear ALL registered usernames!");
        System.out.println("This means all unique user identifiers (like JAMES@23) will be lost.");
        System.out.println("Players will get new IDs when they connect again.");
        System.out.print("Are you sure? (type 'YES' to confirm): ");
        
        scanner.nextLine(); // consume newline
        String confirmation = scanner.nextLine();
        
        if ("YES".equals(confirmation)) {
            clearUserRegistry();
            System.out.println("✅ User registry cleared successfully!");
        } else {
            System.out.println("❌ Operation cancelled.");
        }
    }
    
    /**
     * Clear saved games with confirmation
     */
    private static void clearSavedGamesInteractive(Scanner scanner) {
        System.out.println();
        System.out.println("⚠️  WARNING: This will delete ALL saved chess games!");
        System.out.println("This action cannot be undone. All game progress will be lost.");
        System.out.print("Are you sure? (type 'YES' to confirm): ");
        
        scanner.nextLine(); // consume newline
        String confirmation = scanner.nextLine();
        
        if ("YES".equals(confirmation)) {
            boolean success = clearSavedGames();
            if (success) {
                System.out.println("✅ Saved games cleared successfully!");
            } else {
                System.out.println("❌ Some files could not be deleted. Check permissions.");
            }
        } else {
            System.out.println("❌ Operation cancelled.");
        }
    }
    
    /**
     * Clear both registry and saved games with confirmation
     */
    private static void clearBothInteractive(Scanner scanner) {
        System.out.println();
        System.out.println("⚠️  WARNING: This will completely reset the chess system!");
        System.out.println("- ALL registered usernames will be cleared");
        System.out.println("- ALL saved games will be deleted");
        System.out.println("This is equivalent to a fresh installation.");
        System.out.print("Are you absolutely sure? (type 'RESET' to confirm): ");
        
        scanner.nextLine(); // consume newline
        String confirmation = scanner.nextLine();
        
        if ("RESET".equals(confirmation)) {
            clearUserRegistry();
            boolean gamesCleared = clearSavedGames();
            
            if (gamesCleared) {
                System.out.println("✅ Complete system reset successful!");
                System.out.println("The chess system has been restored to its initial state.");
            } else {
                System.out.println("⚠️  User registry cleared, but some saved games could not be deleted.");
            }
        } else {
            System.out.println("❌ Operation cancelled.");
        }
    }
    
    /**
     * Clear the user registry (programmatic method)
     */
    public static void clearUserRegistry() {
        UserRegistry.clearRegistry();
        System.out.println("User registry cleared.");
    }
    
    /**
     * Clear all saved games (programmatic method)
     * @return true if all files were deleted successfully, false otherwise
     */
    public static boolean clearSavedGames() {
        String saveDir = GameStateManager.getSaveDirectory();
        File saveDirFile = new File(saveDir);
        
        if (!saveDirFile.exists()) {
            System.out.println("Save directory does not exist. Nothing to clear.");
            return true;
        }
        
        boolean allDeleted = true;
        int deletedCount = 0;
        int failedCount = 0;
        
        try {
            File[] files = saveDirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".chess")) {
                        try {
                            if (Files.deleteIfExists(file.toPath())) {
                                deletedCount++;
                                System.out.println("Deleted: " + file.getName());
                            }
                        } catch (IOException e) {
                            System.err.println("Failed to delete: " + file.getName() + " - " + e.getMessage());
                            failedCount++;
                            allDeleted = false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error accessing save directory: " + e.getMessage());
            allDeleted = false;
        }
        
        System.out.println("Deletion summary:");
        System.out.println("  Successfully deleted: " + deletedCount + " files");
        if (failedCount > 0) {
            System.out.println("  Failed to delete: " + failedCount + " files");
        }
        
        return allDeleted;
    }
    
    /**
     * Clear specific saved games by pattern
     * @param namePattern Pattern to match filenames (without .chess extension)
     * @return number of files deleted
     */
    public static int clearSavedGamesByPattern(String namePattern) {
        String saveDir = GameStateManager.getSaveDirectory();
        File saveDirFile = new File(saveDir);
        
        if (!saveDirFile.exists()) {
            return 0;
        }
        
        int deletedCount = 0;
        
        try {
            File[] files = saveDirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".chess")) {
                        String baseName = file.getName().replace(".chess", "");
                        if (baseName.contains(namePattern)) {
                            try {
                                if (Files.deleteIfExists(file.toPath())) {
                                    deletedCount++;
                                    System.out.println("Deleted: " + file.getName());
                                }
                            } catch (IOException e) {
                                System.err.println("Failed to delete: " + file.getName() + " - " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error accessing save directory: " + e.getMessage());
        }
        
        return deletedCount;
    }
    
    /**
     * Backup saved games before clearing
     * @param backupPath Path to backup directory
     * @return true if backup was successful
     */
    public static boolean backupSavedGames(String backupPath) {
        String saveDir = GameStateManager.getSaveDirectory();
        File saveDirFile = new File(saveDir);
        File backupDirFile = new File(backupPath);
        
        if (!saveDirFile.exists()) {
            System.out.println("No save directory to backup.");
            return true;
        }
        
        try {
            // Create backup directory if it doesn't exist
            Files.createDirectories(backupDirFile.toPath());
            
            int backedUpCount = 0;
            File[] files = saveDirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".chess")) {
                        Path sourcePath = file.toPath();
                        Path targetPath = backupDirFile.toPath().resolve(file.getName());
                        
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        backedUpCount++;
                        System.out.println("Backed up: " + file.getName());
                    }
                }
            }
            
            System.out.println("Backup completed: " + backedUpCount + " files backed up to " + backupPath);
            return true;
            
        } catch (IOException e) {
            System.err.println("Backup failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get cleanup statistics
     */
    public static void getCleanupStats() {
        System.out.println("=== Cleanup Statistics ===");
        System.out.println("Current user registrations: " + UserRegistry.getRegisteredUserCount());
        
        String saveDir = GameStateManager.getSaveDirectory();
        File saveDirFile = new File(saveDir);
        
        if (saveDirFile.exists()) {
            File[] saveFiles = saveDirFile.listFiles((dir, name) -> name.endsWith(".chess"));
            int saveCount = saveFiles != null ? saveFiles.length : 0;
            System.out.println("Current saved games: " + saveCount);
            
            if (saveFiles != null && saveFiles.length > 0) {
                long totalSize = 0;
                for (File file : saveFiles) {
                    totalSize += file.length();
                }
                System.out.println("Total save files size: " + formatFileSize(totalSize));
            }
        } else {
            System.out.println("Current saved games: 0 (directory doesn't exist)");
        }
        System.out.println("=========================");
    }
    
    /**
     * Format file size for display
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
