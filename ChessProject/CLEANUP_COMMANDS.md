# Chess System Cleanup Commands

This document provides quick reference commands for cleaning up the chess system.

## 🚀 Quick Start

### Interactive Cleanup Tool
```bash
# Run the interactive cleanup utility
./cleanup.sh
```

### Manual Commands
```bash
# Compile and run directly
javac -cp "lib/*:src/main/java" src/main/java/server/SystemCleanup.java
java -cp "lib/*:src/main/java" server.SystemCleanup
```

## 📋 Available Options

### 1. Clear User Registry Only
- Removes all unique username mappings (e.g., JAMES@23)
- Players will get new IDs when they reconnect
- Saved games remain intact

### 2. Clear Saved Games Only  
- Deletes all `.chess` files from `~/.chess_saves/`
- User registry remains intact
- **⚠️ Cannot be undone!**

### 3. Complete Reset
- Clears both user registry AND saved games
- Equivalent to fresh installation
- **⚠️ Requires typing 'RESET' to confirm**

### 4. Show Current State
- Displays current user count and saved games
- Shows save directory location and file list
- Non-destructive operation

## 🔧 Programmatic Usage

### From Java Code
```java
// Clear user registry
SystemCleanup.clearUserRegistry();

// Clear all saved games
boolean success = SystemCleanup.clearSavedGames();

// Show current state
SystemCleanup.showCurrentState();

// Clear games by pattern
int deleted = SystemCleanup.clearSavedGamesByPattern("JAMES");

// Backup games before clearing
SystemCleanup.backupSavedGames("/path/to/backup");
```

## 📁 File Locations

### User Registry
- **Type**: In-memory storage (static maps)
- **Persistence**: None (cleared on server restart)
- **Content**: Base name → unique username mappings

### Saved Games
- **Location**: `~/.chess_saves/` directory
- **Format**: `*.chess` files
- **Example Files**:
  - `JAMES@23-JOHN@47-MyGame.chess`
  - `SinglePlayerGame.chess`

## 🛡️ Safety Features

### Confirmation Requirements
- **User Registry**: Type `YES` to confirm
- **Saved Games**: Type `YES` to confirm  
- **Complete Reset**: Type `RESET` to confirm

### Backup Options
```bash
# Backup before clearing (manual)
cp -r ~/.chess_saves/ ~/chess_saves_backup_$(date +%Y%m%d_%H%M%S)

# Using the utility
java -cp "lib/*:src/main/java" server.SystemCleanup
# Then use the backup functionality
```

## 🔍 Troubleshooting

### Permission Issues
```bash
# If deletion fails, check permissions
ls -la ~/.chess_saves/
chmod 755 ~/.chess_saves/
chmod 644 ~/.chess_saves/*.chess
```

### Directory Not Found
```bash
# If save directory doesn't exist
mkdir -p ~/.chess_saves
```

### Compilation Issues
```bash
# Ensure all dependencies are available
javac -cp "lib/*:src/main/java" src/main/java/server/*.java
```

## 📊 Usage Examples

### Regular Cleanup (Development)
```bash
# For testing - clear everything
echo "3" | java -cp "lib/*:src/main/java" server.SystemCleanup
# Then type "RESET" when prompted
```

### Selective Cleanup
```bash
# Clear only old games containing "test"
java -c 'SystemCleanup.clearSavedGamesByPattern("test")'
```

### Check Status
```bash
# Quick status check
echo "4" | java -cp "lib/*:src/main/java" server.SystemCleanup
```

## ⚠️ Important Notes

1. **User Registry**: Clearing resets all unique IDs - players get new ones
2. **Saved Games**: Deletion is permanent - no recycle bin
3. **Server Restart**: User registry is automatically cleared on server restart
4. **Multiplayer Impact**: Clearing during active games may cause issues
5. **File Permissions**: Ensure proper read/write access to save directory

## 🎯 Best Practices

- **Backup first** before major cleanups
- **Test with small datasets** before production cleanup
- **Coordinate with users** if clearing during active sessions
- **Use patterns** for selective cleanup rather than clearing everything
- **Monitor disk space** if games accumulate over time
