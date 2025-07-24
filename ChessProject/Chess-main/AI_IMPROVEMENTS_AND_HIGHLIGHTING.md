# AI Improvements and Last Move Highlighting

## Issues Fixed

### 1. **AI Making Poor Hanging Moves**
- **Problem**: AI was moving valuable pieces (like bishops) to unprotected squares where they could be easily captured
- **Root Cause**: Insufficient hanging piece detection in move evaluation
- **Impact**: AI was losing material unnecessarily due to poor positional judgment

### 2. **Missing Last Move Visualization**
- **Problem**: No visual indicator of the most recent move made
- **Impact**: Difficult to track game progress and see what the opponent just did

## Solutions Implemented

### 1. **Enhanced Hanging Piece Detection**

#### A. New `createHangingPiece()` Method
```java
private boolean createHangingPiece(Move move, Board board, Team team) {
    // Simulates the move and checks if opponent can capture for free
    // Returns true if the move creates a hanging piece
}
```

**Key Features:**
- Simulates the proposed move
- Switches to opponent's perspective 
- Checks if opponent can capture the moved piece
- Validates if the piece would be adequately protected
- Provides detailed logging for debugging

#### B. Improved Move Ordering with Hanging Penalty
```java
// CRITICAL: Heavy penalty for moves that create hanging pieces
if (createHangingPiece(move, board, team)) {
    int pieceValue = move.getMovedPiece().getPoints();
    score -= pieceValue * 100; // MASSIVE penalty for hanging pieces
}
```

**Impact:**
- 100x penalty for hanging pieces (e.g., -300 for hanging bishop)
- Makes hanging moves extremely unlikely to be selected
- Debug output shows when hanging moves are detected

#### C. Enhanced Pruning Logic
```java
// 1. CRITICAL: Prune moves that create hanging pieces (except for good captures)
if (createHangingPiece(move, board, team)) {
    // Only allow if capturing something significantly more valuable
    if (capturedValue > attackerValue + 1) {
        return false; // Allow favorable capture even if piece hangs
    }
    return true; // Prune hanging moves
}
```

**Benefits:**
- Eliminates hanging moves before detailed evaluation
- Allows hanging moves only for significantly favorable captures
- Improves AI performance by reducing bad move evaluation

### 2. **Last Move Highlighting System**

#### A. Move Tracking in Table Class
```java
private Move lastMove = null; // Track the last move made

// In AI move execution:
lastMove = aiMove;

// In human move execution (TilePanel):
table.setLastMove(move);
```

#### B. Visual Highlighting in TilePanel
```java
// Highlight last move (from and to squares)
if (this.coordinate.equals(fromCoord)) {
    // Light yellow for the square the piece moved from
    this.setBackground(new Color(255, 255, 180));
} else if (this.coordinate.equals(toCoord)) {
    // Darker yellow for the square the piece moved to
    this.setBackground(new Color(255, 255, 120));
}
```

**Visual Design:**
- **From Square**: Light yellow (255, 255, 180) - where piece moved from
- **To Square**: Darker yellow (255, 255, 120) - where piece moved to
- **Priority Order**: Check (red) > Selected (green) > Last move (yellow) > Default

## Technical Implementation

### Files Modified

1. **`ChessAI.java`**
   - Added `createHangingPiece()` method for hanging detection
   - Enhanced `calculateMoveOrderingScore()` with hanging penalties
   - Improved `pruneEarly()` method for better move filtering

2. **`Table.java`**
   - Added `lastMove` tracking variable
   - Updated AI move execution to track moves
   - Added getter/setter methods for last move

3. **`TilePanel.java`**
   - Updated human move execution to track moves
   - Enhanced `assignTileColor()` to highlight last move
   - Proper priority ordering for different highlights

### AI Evaluation Improvements

#### Before (Problematic):
```
Bishop move to hanging square: -0.3 penalty (barely noticeable)
AI would frequently choose hanging moves
```

#### After (Fixed):
```
Bishop move to hanging square: -300 penalty (massive deterrent)
AI avoids hanging moves unless capturing something very valuable
```

### Hanging Detection Logic

1. **Simulate Proposed Move**: Create test board with move applied
2. **Switch Perspective**: Look from opponent's viewpoint
3. **Check Captures**: Find all opponent moves that capture our piece
4. **Evaluate Protection**: Determine if piece would be adequately defended
5. **Apply Penalty**: Heavily penalize unprotected hanging pieces

### Last Move Highlighting Priority

1. **Check Highlight** (Red) - King in check (highest priority)
2. **Selected Piece** (Green) - Currently selected piece
3. **Last Move** (Yellow) - Most recent move made
4. **Default Colors** - Normal board pattern

## Benefits

### 1. **Improved AI Strategy**
- **No More Hanging Pieces**: AI will avoid obvious hanging moves
- **Better Tactical Awareness**: Enhanced threat detection
- **Smarter Captures**: Only hangs pieces for significantly favorable trades
- **Debug Visibility**: Console output shows hanging detection in action

### 2. **Enhanced User Experience**
- **Visual Move Tracking**: Easy to see what move was just made
- **Better Game Flow**: Clear indication of recent activity
- **Non-Intrusive Design**: Highlighting doesn't interfere with gameplay
- **Consistent Across Modes**: Works in both AI and multiplayer modes

## Testing

### To Verify AI Improvements:
1. **Start AI Game**: Create new AI game (player as white)
2. **Create Threats**: Position pieces to threaten AI pieces
3. **Observe Behavior**: AI should avoid moving threatened pieces to unprotected squares
4. **Check Console**: Look for "HANGING DETECTION" and "PRUNING" messages
5. **Monitor Material**: AI should no longer lose pieces carelessly

### To Verify Last Move Highlighting:
1. **Make Moves**: Make moves in AI or multiplayer mode
2. **Check Highlighting**: Last move should be highlighted in yellow
3. **Verify Colors**: From square (light yellow), to square (darker yellow)
4. **Test Priority**: Selected pieces should show green over yellow

## Console Debug Output

The enhanced AI provides helpful debug information:
```
HANGING DETECTION: Bishop would be hanging at [3, 4]!
MOVE ORDERING: SEVERE penalty for hanging Bishop worth 3 points!
PRUNING: Hanging Bishop move
```

This helps developers and players understand AI decision-making.

## Future Considerations

- **Difficulty Levels**: Could adjust hanging penalty based on AI difficulty
- **Advanced Threats**: Could extend to detect more complex tactical threats
- **Move History**: Could extend highlighting to show multiple recent moves
- **Animation**: Could add smooth transitions for move highlighting
