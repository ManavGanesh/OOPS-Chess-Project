# Enhanced Load Game Functionality

## Overview
The chess game now features an enhanced load game functionality that respects the original game type and provides appropriate restrictions and options when loading saved games.

## Features

### 1. Game Type Detection
- **AI Games**: Games originally played against the AI can only be loaded in offline mode
- **Multiplayer Games**: Games originally played with another player can only be loaded in multiplayer mode
- The system automatically detects the game type based on the saved game state

### 2. Mode Selection Dialog
When a player clicks "Load Game" and selects a saved game, a new dialog appears asking:
- **"Load in Offline Mode"**: For AI games or when you want to play against AI
- **"Load in Multiplayer Mode"**: For multiplayer games or when you want to find an opponent

### 3. Smart Restrictions
- **AI Games**: The "Multiplayer Mode" button is disabled with a tooltip explaining that AI games can only be played offline
- **Multiplayer Games**: The "Offline Mode" button is disabled with a tooltip explaining that multiplayer games require an opponent

### 4. Enhanced Game State
The `GameState` class now includes:
- `isAIGame()` field to track the original game type
- Backward compatibility with existing save files
- Enhanced display format showing game type: `"SaveName (Player1 vs Player2) [AI/Multiplayer] - Date"`

## How It Works

### Loading Process
1. Player clicks "Load Game" from the main menu
2. Player selects a saved game from the list
3. System shows the **LoadGameModeDialog** with appropriate restrictions
4. Based on selection:
   - **Offline Mode**: Loads the game and starts AI mode (if AI game) or offline multiplayer mode
   - **Multiplayer Mode**: Connects to server, sends load game request, and waits for opponent

### Server Integration
- Load game requests are sent to the server when loading multiplayer games
- The server forwards the request to paired opponents
- If no opponent is paired, the system attempts to find one through the normal pairing process

### Save Game Integration
- When saving games, the system now tracks whether it's an AI or multiplayer game
- This information is stored in the save file and used when loading

## Technical Implementation

### New Classes
- `LoadGameModeDialog`: Dialog for choosing load mode
- Enhanced `GameState`: Added `isAIGame` field and backward compatibility

### Modified Classes
- `LoadGameDialog`: Now uses the mode selection dialog
- `Table`: Enhanced load game logic with server integration
- `GameLogic`: Updated save game method to include game type
- `ClientListenThread`: Enhanced load game message handling
- `Server.ClientListenThread`: Enhanced load game request handling

### Message Types
- `LOAD_GAME`: Used for load game requests and responses between client and server

## Usage Examples

### Loading an AI Game
1. Click "Load Game"
2. Select an AI game from the list
3. Only "Load in Offline Mode" is available
4. Game loads and you can continue playing against AI

### Loading a Multiplayer Game
1. Click "Load Game"
2. Select a multiplayer game from the list
3. Only "Load in Multiplayer Mode" is available
4. System connects to server and attempts to find opponent
5. When opponent joins, the game continues from the saved state

## Benefits
- **Type Safety**: Prevents loading AI games in multiplayer mode and vice versa
- **User Experience**: Clear options and restrictions prevent confusion
- **Server Integration**: Seamless multiplayer game loading with opponent pairing
- **Backward Compatibility**: Existing save files continue to work
- **Enhanced Display**: Save file list shows game type for better identification 