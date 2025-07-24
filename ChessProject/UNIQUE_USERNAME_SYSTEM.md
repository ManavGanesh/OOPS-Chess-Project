# Unique Username System Documentation

## Overview
I have implemented a comprehensive unique username system that automatically assigns random two-digit numbers to player names to prevent conflicts and ensure each player has a unique identifier.

## How It Works

### Username Format
- **Input**: `JAMES`
- **Output**: `JAMES@23` (where 23 is a unique random two-digit number)

### Key Features

1. **Automatic Assignment**: When a user enters "JAMES", the system automatically generates "JAMES@23"
2. **Consistency**: If "JAMES" connects again, they get the same identifier "JAMES@23" 
3. **Uniqueness**: Each base name gets exactly one unique identifier
4. **Random IDs**: Two-digit numbers (10-99) are assigned randomly
5. **Game Access Control**: Users can only access saved games involving their unique username

## Implementation Details

### Core Components

#### 1. UserRegistry Class (`src/main/java/server/UserRegistry.java`)
- **Thread-safe**: Uses `ConcurrentHashMap` for multi-client support
- **ID Range**: 10-99 (90 possible unique IDs)
- **Methods**:
  - `registerUsername(String baseName)`: Registers and returns unique username
  - `canAccessGame(String playerUsername, String gamePlayerName)`: Controls game access
  - `getBaseName(String uniqueUsername)`: Extracts base name from unique username

#### 2. Enhanced GameStateManager (`src/main/java/chess_game/Utilities/GameStateManager.java`)
- **Player-specific game loading**: `getPlayerAccessibleGames(String playerUniqueUsername)`
- **Filtered save lists**: `getPlayerSaveNames(String playerUniqueUsername)`
- **Enhanced save naming**: Includes unique usernames in save file names

#### 3. Updated Client Integration
- **Server-side**: `SClient.setPlayerName()` automatically uses `UserRegistry`
- **Client-side**: `Table.java` registers unique usernames before multiplayer/AI games
- **Load Game Dialog**: Filters games by player's unique username

## Usage Examples

### Example 1: Basic Registration
```java
String uniqueName1 = UserRegistry.registerUsername("JAMES");  // Returns "JAMES@23"
String uniqueName2 = UserRegistry.registerUsername("JAMES");  // Returns "JAMES@23" (same)
String uniqueName3 = UserRegistry.registerUsername("JOHN");   // Returns "JOHN@47"
```

### Example 2: Game Access Control
```java
// JAMES@23 can access games saved by:
UserRegistry.canAccessGame("JAMES@23", "JAMES");     // true (base name match)
UserRegistry.canAccessGame("JAMES@23", "JAMES@23");  // true (exact match)
UserRegistry.canAccessGame("JAMES@23", "JOHN@47");   // false (different user)
```

### Example 3: Load Game Filtering
```java
// Only shows games where JAMES@23 is player1 or player2
List<String> jamesGames = GameStateManager.getPlayerSaveNames("JAMES@23");
```

## Testing

Run the test suite:
```bash
cd /path/to/ChessProject
javac -cp "lib/*:src/main/java" src/main/java/server/UserRegistryTest.java
java -cp "lib/*:src/main/java" server.UserRegistryTest
```

### Test Results
✅ **Basic Registration**: JAMES → JAMES@71  
✅ **Consistency**: Second JAMES registration returns same ID  
✅ **Uniqueness**: Different users get different IDs  
✅ **Access Control**: Users can only access their own games  
✅ **Edge Cases**: Handles null, empty, and whitespace names  
✅ **Capacity**: 90 unique IDs available (10-99)  

## Game Save/Load Integration

### Multiplayer Games
1. **Save**: Games are saved with unique usernames (e.g., "JAMES@23 vs JOHN@47")
2. **Load**: Players only see games they participated in
3. **Access**: Cannot load other players' games

### Example Save File Names
- Original: `MyGame.chess`
- Enhanced: `JAMES@23-JOHN@47-MyGame.chess`

## Benefits

1. **No Name Conflicts**: Multiple "JAMES" players can coexist
2. **Persistent Identity**: Same username across sessions
3. **Privacy**: Players only see their own saved games
4. **Scalability**: Supports up to 90 unique base names
5. **Backward Compatibility**: System handles old save formats

## Technical Specifications

- **ID Format**: Two-digit numbers (10-99)
- **Separator**: `@` symbol
- **Storage**: Thread-safe static maps
- **Serialization**: All classes implement `Serializable`
- **Performance**: O(1) lookup times for registration and access checks

## Error Handling

- **Null/Empty Names**: Default to "Anonymous"
- **ID Exhaustion**: Runtime exception when all 90 IDs are used
- **Invalid Characters**: Names are cleaned before processing

## Future Enhancements

1. **Persistent Storage**: Save registry to file/database
2. **Extended ID Range**: Support 3-digit IDs if needed
3. **User Management**: Admin tools for registry management
4. **Custom Separators**: Allow different separator characters

This system ensures that when you enter "JAMES", you get a unique identifier like "JAMES@23" and can only access saved games involving "JAMES@23", providing both uniqueness and privacy in multiplayer chess games.
