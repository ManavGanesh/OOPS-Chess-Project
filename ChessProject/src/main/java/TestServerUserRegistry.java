import server.UserRegistry;

/**
 * Test server-side username registration
 */
public class TestServerUserRegistry {
    public static void main(String[] args) {
        System.out.println("=== Testing Server-Side Username Registry ===");
        
        // Test 1: Register first username
        String user1 = UserRegistry.registerUsername("Player1");
        System.out.println("Register 'Player1': " + (user1 != null ? "SUCCESS (" + user1 + ")" : "FAILED"));
        
        // Test 2: Register second username
        String user2 = UserRegistry.registerUsername("Player2");
        System.out.println("Register 'Player2': " + (user2 != null ? "SUCCESS (" + user2 + ")" : "FAILED"));
        
        // Test 3: Try to register duplicate username
        String user3 = UserRegistry.registerUsername("Player1");
        System.out.println("Register 'Player1' (duplicate): " + (user3 != null ? "FAILED - should not succeed" : "SUCCESS - correctly rejected"));
        
        // Test 4: Check registry state
        System.out.println("Active usernames: " + UserRegistry.getActiveUsernames());
        System.out.println("Registered user count: " + UserRegistry.getRegisteredUserCount());
        
        // Test 5: Unregister users
        boolean unregistered1 = UserRegistry.unregisterUsername("Player1");
        boolean unregistered2 = UserRegistry.unregisterUsername("Player2");
        System.out.println("Unregister 'Player1': " + (unregistered1 ? "SUCCESS" : "FAILED"));
        System.out.println("Unregister 'Player2': " + (unregistered2 ? "SUCCESS" : "FAILED"));
        
        // Test 6: Final state
        System.out.println("Final active usernames: " + UserRegistry.getActiveUsernames());
        System.out.println("Final registered user count: " + UserRegistry.getRegisteredUserCount());
        
        System.out.println("\n=== Test Complete ===");
    }
}
