package server;

/**
 * Test class to demonstrate and verify the UserRegistry functionality
 */
public class UserRegistryTest {
    
    public static void main(String[] args) {
        System.out.println("=== UserRegistry Test ===");
        System.out.println();
        
        // Test 1: Basic username registration
        System.out.println("Test 1: Basic username registration");
        String james1 = UserRegistry.registerUsername("JAMES");
        String james2 = UserRegistry.registerUsername("JAMES"); // Should return null (already taken)
        String john = UserRegistry.registerUsername("JOHN");
        String mary = UserRegistry.registerUsername("MARY");
        
        System.out.println("First JAMES registration: " + james1);
        System.out.println("Second JAMES registration: " + james2 + " (should be null - already taken)");
        System.out.println("JOHN registration: " + john);
        System.out.println("MARY registration: " + mary);
        System.out.println("Second JAMES registration failed? " + (james2 == null));
        System.out.println();
        
        // Test 2: Multiple different users
        System.out.println("Test 2: Multiple different users");
        String alice = UserRegistry.registerUsername("ALICE");
        String bob = UserRegistry.registerUsername("BOB");
        String charlie = UserRegistry.registerUsername("CHARLIE");
        
        System.out.println("ALICE: " + alice);
        System.out.println("BOB: " + bob);
        System.out.println("CHARLIE: " + charlie);
        System.out.println();
        
        // Test 3: Access control for saved games
        System.out.println("Test 3: Game access control");
        System.out.println("Can " + james1 + " access game saved by JAMES? " + 
                         UserRegistry.canAccessGame(james1, "JAMES"));
        System.out.println("Can " + james1 + " access game saved by " + james1 + "? " + 
                         UserRegistry.canAccessGame(james1, james1));
        System.out.println("Can " + john + " access game saved by " + james1 + "? " + 
                         UserRegistry.canAccessGame(john, james1));
        System.out.println("Can " + alice + " access game saved by ALICE? " + 
                         UserRegistry.canAccessGame(alice, "ALICE"));
        System.out.println();
        
        // Test 4: Edge cases
        System.out.println("Test 4: Edge cases");
        String empty = UserRegistry.registerUsername("");
        String nullName = UserRegistry.registerUsername(null);
        String spaces = UserRegistry.registerUsername("  TEST  ");
        
        System.out.println("Empty string: " + empty + " (should be null)");
        System.out.println("Null name: " + nullName + " (should be null)");
        System.out.println("Name with spaces: " + spaces + " (should be 'TEST')");
        System.out.println();
        
        // Test 5: Registry state
        System.out.println("Test 5: Registry state");
        UserRegistry.printRegistryState();
        System.out.println();
        
        // Test 6: Stress test (register many users)
        System.out.println("Test 6: Stress test - registering many users");
        int initialCount = UserRegistry.getRegisteredUserCount();
        for (int i = 1; i <= 10; i++) {
            String username = UserRegistry.registerUsername("USER" + i);
            System.out.println("USER" + i + " -> " + username);
        }
        int finalCount = UserRegistry.getRegisteredUserCount();
        System.out.println("Users added: " + (finalCount - initialCount));
        System.out.println("Total registered users: " + finalCount);
        System.out.println();
        
        // Test 7: Display name extraction
        System.out.println("Test 7: Display name extraction");
        System.out.println("Display name for " + james1 + ": " + UserRegistry.getDisplayName(james1));
        System.out.println("Display name for ALICE: " + UserRegistry.getDisplayName("ALICE"));
        System.out.println("Display name for " + alice + ": " + UserRegistry.getDisplayName(alice));
        System.out.println();
        
        System.out.println("=== Test Complete ===");
    }
}
