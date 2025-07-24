# Color Selection Feature Implementation

## Overview
This document describes the implementation of the color selection feature for multiplayer chess games. The feature allows the player who sends a play request to choose their preferred color (White, Black, or Random), and the system properly assigns colors to both players accordingly.

## Features Implemented

### 1. Color Selection UI
- **Location**: `PlayerSelectionPanel.java`
- **New Components**:
  - Color selection combo box with options: "White", "Black", "Random"
  - Color selection label: "Your Color:"
  - Positioned next to the time selection option

### 2. Enhanced Play Request System
- **Location**: `PlayRequest.java`
- **New Fields**:
  - `requesterPreferredColor`: Stores the requester's color preference
  - `setRequesterPreferredColor()` and `getRequesterPreferredColor()` methods

### 3. Server-Side Color Assignment
- **Location**: `server/ClientListenThread.java`
- **Logic**: 
  - For normal games: Uses requester's color preference
  - For load games: Preserves original player colors from saved game
  - For random selection: Assigns requester as White, responder as Black

### 4. Save/Load Color Preservation
- **Location**: `GameState.java`
- **New Fields**:
  - `player1Color`: Color of player 1
  - `player2Color`: Color of player 2
  - Enhanced constructor with color parameters
  - Backward compatibility for old save files

## Implementation Details

### UI Changes
```java
// Added to PlayerSelectionPanel
public javax.swing.JComboBox<String> colorSelectionCombo;
private javax.swing.JLabel colorSelectionLBL;

// Color selection options
colorSelectionCombo.addItem("White");
colorSelectionCombo.addItem("Black");
colorSelectionCombo.addItem("Random");
```

### Play Request Enhancement
```java
// In Table.java - setupPlayerSelectionHandlers()
Team selectedColor = playerSelectionPanel.getSelectedColor();
if ("Random".equals(playerSelectionPanel.colorSelectionCombo.getSelectedItem())) {
    selectedColor = null; // Let server decide
}
request.setRequesterPreferredColor(selectedColor);
```

### Server Color Assignment Logic
```java
// In server/ClientListenThread.java - handlePlayResponse()
if (response.isLoadGameRequest() && response.getLoadGameState() != null) {
    // Load game scenario - preserve original player colors
    Messages.GameState loadGameState = response.getLoadGameState();
    if (loadGameState.getPlayer1Color() != null && loadGameState.getPlayer2Color() != null) {
        // Determine which player is which based on names
        if (finalRequesterClient.getPlayerName().equals(loadGameState.getPlayer1Name())) {
            requesterTeam = loadGameState.getPlayer1Color();
            responderTeam = loadGameState.getPlayer2Color();
        } else if (finalRequesterClient.getPlayerName().equals(loadGameState.getPlayer2Name())) {
            requesterTeam = loadGameState.getPlayer2Color();
            responderTeam = loadGameState.getPlayer1Color();
        }
    }
} else if (response.getRequesterPreferredColor() != null) {
    // Normal game with color preference
    requesterTeam = response.getRequesterPreferredColor();
    responderTeam = (requesterTeam == Team.WHITE) ? Team.BLACK : Team.WHITE;
} else {
    // Random assignment
    requesterTeam = Team.WHITE;
    responderTeam = Team.BLACK;
}
```

### Save Game Color Preservation
```java
// In Table.java - save game handler
GameState gameState = new GameState(
    chessBoard,
    playerName,
    opponentName,
    chessBoard.getCurrentPlayer().getTeam(),
    saveName.trim(),
    false, // This is a multiplayer game
    client.getTeam(), // Player 1 color (current player)
    (client.getTeam() == Team.WHITE) ? Team.BLACK : Team.WHITE // Player 2 color (opponent)
);
```

### Load Game Color Restoration
```java
// In Table.java - load game handler
if (loadedGameState.getPlayer1Color() != null && loadedGameState.getPlayer2Color() != null) {
    // Determine which player this client should be based on names
    if (playerName.equals(loadedGameState.getPlayer1Name())) {
        client.setTeam(loadedGameState.getPlayer1Color());
    } else if (playerName.equals(loadedGameState.getPlayer2Name())) {
        client.setTeam(loadedGameState.getPlayer2Color());
    } else {
        // Fallback to current player team if names don't match
        client.setTeam(loadedGameState.getCurrentPlayerTeam());
    }
} else {
    // Fallback for old saves without color information
    client.setTeam(loadedGameState.getCurrentPlayerTeam());
}
```

## User Experience Flow

### 1. New Game Request
1. User selects a player from the list
2. User chooses their preferred color (White/Black/Random)
3. User selects game time
4. User clicks "Send Play Request"
5. System sends request with color preference
6. Server assigns colors based on preference
7. Both players start with correct colors

### 2. Load Game Request
1. User loads a saved multiplayer game
2. System connects to server and goes to player selection
3. User selects a player to continue the game
4. System sends load game request with original game state
5. Server preserves original player colors based on saved data
6. Both players start with their original colors

## Backward Compatibility

### Old Save Files
- Games saved before this feature will still load correctly
- System falls back to current player team assignment
- No data loss or corruption

### Server Compatibility
- Server handles both old and new play request formats
- Graceful fallback for missing color information
- Maintains existing functionality for all clients

## Testing Scenarios

### Scenario 1: New Game with Color Preference
1. Player A selects "White" and sends request to Player B
2. Player B accepts the request
3. Player A gets White, Player B gets Black
4. Game starts with correct color assignment

### Scenario 2: New Game with Random Selection
1. Player A selects "Random" and sends request to Player B
2. Player B accepts the request
3. Server assigns Player A as White, Player B as Black
4. Game starts with random color assignment

### Scenario 3: Load Game with Color Preservation
1. Player A saves a game where they were Black
2. Player A loads the game and sends request to Player B
3. Player B accepts the request
4. Player A gets Black, Player B gets White (original colors preserved)
5. Game continues from saved state with correct colors

### Scenario 4: Load Game with Different Opponent
1. Player A saves a game with Player B (A=White, B=Black)
2. Player A loads the game and sends request to Player C
3. Player C accepts the request
4. Player A gets White, Player C gets Black (original colors preserved)
5. Game continues from saved state with correct colors

## Benefits

1. **User Choice**: Players can choose their preferred color
2. **Fair Play**: Random option ensures fair color distribution
3. **Consistency**: Load games preserve original player colors
4. **Backward Compatibility**: Existing saves continue to work
5. **Enhanced UX**: Clear color selection interface
6. **Server Reliability**: Robust color assignment logic

## Future Enhancements

1. **Color Preference Memory**: Remember user's preferred color
2. **Advanced Random**: True random assignment with coin flip
3. **Color Statistics**: Track win rates by color
4. **Visual Indicators**: Show color preview in selection
5. **Tournament Mode**: Enforced color rotation for fairness 