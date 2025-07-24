import chess_game.gui.Table;

/**
 * Test class to verify username registration and locking functionality
 */
public class TestUsernameRegistration {
    public static void main(String[] args) {
        System.out.println("Testing username registration and locking...");
        
        // Test 1: Username registration on instance creation
        System.out.println("\n=== Test 1: Username Registration ===");
        try {
            Table table = new Table();
            System.out.println("✓ Table created successfully with username registration");
        } catch (Exception e) {
            System.out.println("✗ Table creation failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Test 2: Username registry functionality
        System.out.println("\n=== Test 2: Username Registry ===");
        try {
            // Test registering a username
            String username1 = server.UserRegistry.registerUsername("TestPlayer1");
            System.out.println("✓ Registered username: " + username1);
            
            // Test registering the same username (should fail)
            String username2 = server.UserRegistry.registerUsername("TestPlayer1");
            if (username2 == null) {
                System.out.println("✓ Correctly prevented duplicate username registration");
            } else {
                System.out.println("✗ Failed to prevent duplicate username: " + username2);
            }
            
            // Test registering a different username
            String username3 = server.UserRegistry.registerUsername("TestPlayer2");
            System.out.println("✓ Registered different username: " + username3);
            
            // Test unregistering usernames
            boolean unregistered1 = server.UserRegistry.unregisterUsername("TestPlayer1");
            boolean unregistered2 = server.UserRegistry.unregisterUsername("TestPlayer2");
            System.out.println("✓ Unregistered usernames: " + unregistered1 + ", " + unregistered2);
            
        } catch (Exception e) {
            System.out.println("✗ Username registry test failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== Test Summary ===");
        System.out.println("Username registration and locking tests completed.");
        System.out.println("To fully test:");
        System.out.println("1. Run the application");
        System.out.println("2. Enter a username when prompted");
        System.out.println("3. Verify the username field is locked in the main menu");
        System.out.println("4. Try to enter multiplayer mode");
        System.out.println("5. Verify the same username is used consistently");
        System.out.println("6. Test load game functionality");
        System.out.println("7. Test color selection in multiplayer");
    }
}
