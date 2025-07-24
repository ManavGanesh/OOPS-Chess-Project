package server;

/**
 * Quick command-line cleanup utility for power users.
 * Accepts command line arguments for non-interactive cleanup.
 */
public class QuickCleanup {
    
    public static void main(String[] args) {
        if (args.length == 0) {
            showUsage();
            return;
        }
        
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "status":
            case "show":
            case "state":
                SystemCleanup.showCurrentState();
                break;
                
            case "clear-users":
            case "clear-registry":
                if (confirmDangerous(args)) {
                    SystemCleanup.clearUserRegistry();
                    System.out.println("✅ User registry cleared!");
                } else {
                    System.out.println("❌ Add --force flag to confirm this action.");
                }
                break;
                
            case "clear-games":
            case "clear-saves":
                if (confirmDangerous(args)) {
                    boolean success = SystemCleanup.clearSavedGames();
                    if (success) {
                        System.out.println("✅ Saved games cleared!");
                    } else {
                        System.out.println("⚠️ Some files could not be deleted.");
                    }
                } else {
                    System.out.println("❌ Add --force flag to confirm this action.");
                }
                break;
                
            case "clear-all":
            case "reset":
                if (confirmDangerous(args)) {
                    SystemCleanup.clearUserRegistry();
                    boolean success = SystemCleanup.clearSavedGames();
                    if (success) {
                        System.out.println("✅ Complete system reset successful!");
                    } else {
                        System.out.println("⚠️ Registry cleared, but some game files could not be deleted.");
                    }
                } else {
                    System.out.println("❌ Add --force flag to confirm this dangerous action.");
                }
                break;
                
            case "clear-pattern":
                if (args.length < 2) {
                    System.out.println("❌ Pattern required. Usage: clear-pattern <pattern>");
                    return;
                }
                String pattern = args[1];
                int deleted = SystemCleanup.clearSavedGamesByPattern(pattern);
                System.out.println("✅ Deleted " + deleted + " games matching pattern: " + pattern);
                break;
                
            case "backup":
                if (args.length < 2) {
                    System.out.println("❌ Backup path required. Usage: backup <path>");
                    return;
                }
                String backupPath = args[1];
                boolean success = SystemCleanup.backupSavedGames(backupPath);
                if (success) {
                    System.out.println("✅ Backup completed to: " + backupPath);
                } else {
                    System.out.println("❌ Backup failed.");
                }
                break;
                
            case "stats":
                SystemCleanup.getCleanupStats();
                break;
                
            case "help":
            case "--help":
            case "-h":
                showUsage();
                break;
                
            default:
                System.out.println("❌ Unknown command: " + command);
                showUsage();
        }
    }
    
    private static boolean confirmDangerous(String[] args) {
        for (String arg : args) {
            if ("--force".equals(arg) || "-f".equals(arg)) {
                return true;
            }
        }
        return false;
    }
    
    private static void showUsage() {
        System.out.println("🧹 Chess Quick Cleanup Utility");
        System.out.println("==============================");
        System.out.println();
        System.out.println("USAGE:");
        System.out.println("  java server.QuickCleanup <command> [options]");
        System.out.println();
        System.out.println("COMMANDS:");
        System.out.println("  status              Show current system state");
        System.out.println("  clear-users --force Clear user registry (requires --force)");
        System.out.println("  clear-games --force Clear saved games (requires --force)");
        System.out.println("  clear-all --force   Complete reset (requires --force)");
        System.out.println("  clear-pattern <pat> Clear games matching pattern");
        System.out.println("  backup <path>       Backup saved games to path");
        System.out.println("  stats               Show detailed statistics");
        System.out.println("  help                Show this help");
        System.out.println();
        System.out.println("OPTIONS:");
        System.out.println("  --force, -f         Force dangerous operations");
        System.out.println();
        System.out.println("EXAMPLES:");
        System.out.println("  java server.QuickCleanup status");
        System.out.println("  java server.QuickCleanup clear-users --force");
        System.out.println("  java server.QuickCleanup clear-pattern \"test\"");
        System.out.println("  java server.QuickCleanup backup ~/chess_backup");
        System.out.println();
        System.out.println("⚠️  WARNING: --force operations cannot be undone!");
    }
}
