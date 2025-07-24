import server.UserRegistry;
import Messages.UsernameRegisterMessage;
import Messages.PlayerInfo;
import Messages.Message;
import java.util.ArrayList;

/**
 * Test to verify multiplayer player list functionality
 */
public class TestPlayerListFunctionality {
    public static void main(String[] args) {
        System.out.println("=== Testing Player List Functionality ===");
        
        // Test 1: Username registration
        System.out.println("\n1. Testing username registration:");
        String user1 = UserRegistry.registerUsername("Player1");
        String user2 = UserRegistry.registerUsername("Player2");
        System.out.println("   - Registered Player1: " + (user1 != null ? "SUCCESS" : "FAILED"));
        System.out.println("   - Registered Player2: " + (user2 != null ? "SUCCESS" : "FAILED"));
        
        // Test 2: Duplicate username check
        System.out.println("\n2. Testing duplicate username prevention:");
        String user3 = UserRegistry.registerUsername("Player1");
        System.out.println("   - Duplicate Player1: " + (user3 == null ? "SUCCESS (correctly rejected)" : "FAILED (should be rejected)"));
        
        // Test 3: PlayerInfo creation
        System.out.println("\n3. Testing PlayerInfo creation:");
        try {
            PlayerInfo player1Info = new PlayerInfo("Player1", "client-id-1");
            PlayerInfo player2Info = new PlayerInfo("Player2", "client-id-2");
            System.out.println("   - PlayerInfo creation: SUCCESS");
            System.out.println("   - Player1 info: " + player1Info.playerName + " (ID: " + player1Info.playerId + ")");
            System.out.println("   - Player2 info: " + player2Info.playerName + " (ID: " + player2Info.playerId + ")");
        } catch (Exception e) {
            System.out.println("   - PlayerInfo creation: FAILED - " + e.getMessage());
        }
        
        // Test 4: ArrayList of PlayerInfo
        System.out.println("\n4. Testing PlayerInfo list:");
        try {
            ArrayList<PlayerInfo> playerList = new ArrayList<>();
            playerList.add(new PlayerInfo("Player1", "client-id-1"));
            playerList.add(new PlayerInfo("Player2", "client-id-2"));
            System.out.println("   - PlayerInfo list creation: SUCCESS");
            System.out.println("   - List size: " + playerList.size());
            for (PlayerInfo player : playerList) {
                System.out.println("   - Player: " + player.playerName + " (ID: " + player.playerId + ")");
            }
        } catch (Exception e) {
            System.out.println("   - PlayerInfo list creation: FAILED - " + e.getMessage());
        }
        
        // Test 5: Message serialization
        System.out.println("\n5. Testing message serialization:");
        try {
            Message msg = new Message(Message.MessageTypes.PLAYER_LIST);
            ArrayList<PlayerInfo> playerList = new ArrayList<>();
            playerList.add(new PlayerInfo("Player1", "client-id-1"));
            msg.content = playerList;
            System.out.println("   - Message creation: SUCCESS");
            System.out.println("   - Message type: " + msg.type);
            System.out.println("   - Message content: " + (msg.content != null ? "ArrayList with " + ((ArrayList<PlayerInfo>)msg.content).size() + " players" : "null"));
        } catch (Exception e) {
            System.out.println("   - Message creation: FAILED - " + e.getMessage());
        }
        
        // Test 6: Username register message
        System.out.println("\n6. Testing username register message:");
        try {
            UsernameRegisterMessage registerMsg = new UsernameRegisterMessage("TestPlayer");
            System.out.println("   - Username register message creation: SUCCESS");
            System.out.println("   - Requested username: " + registerMsg.getRequestedUsername());
            
            // Test response
            UsernameRegisterMessage responseMsg = new UsernameRegisterMessage("TestPlayer", "TestPlayer", true, null);
            System.out.println("   - Response message creation: SUCCESS");
            System.out.println("   - Response success: " + responseMsg.isSuccess());
            System.out.println("   - Registered username: " + responseMsg.getRegisteredUsername());
        } catch (Exception e) {
            System.out.println("   - Username register message creation: FAILED - " + e.getMessage());
        }
        
        // Cleanup
        UserRegistry.clearRegistry();
        System.out.println("\n=== All Tests Complete ===");
        System.out.println("If all tests passed, the issue is likely in the network communication");
        System.out.println("or the timing of when player list requests are sent.");
    }
}
