/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chess_game.gui;

import ClientSide.Client;
import Messages.Message;
import Messages.MovementMessage;
import chess_game.Boards.Board;
import chess_game.Boards.Tile;
import chess_game.Pieces.Coordinate;
import chess_game.Move.Move;
import chess_game.Pieces.PieceTypes;
import chess_game.Pieces.Team;
import java.awt.GridBagLayout;
import javax.swing.JPanel;
import chess_game.Resources.BOARD_Configurations;
import chess_game.Resources.GUI_Configurations;
import chess_game.Utilities.BoardUtilities;
import chess_game.Utilities.MoveUtilities;
import chess_game.Utilities.GameLogic;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class TilePanel extends JPanel {

    Coordinate coordinate;
    JLabel pieceIcon;
    private BoardPanel boardPanel;
    private Client client;
    
    // Drag and drop fields
    private boolean isDragging = false;
    private Point dragStartPoint;
    private Tile draggedTile;
    private JLabel draggedPieceIcon;
    private Point initialIconPosition;
    private Board chessBoard; // Reference for cleanup
    private javax.swing.Timer dragTimeoutTimer; // Safety timer to prevent stuck drags

    public TilePanel(BoardPanel boardPanel, Coordinate coord, Board chessBoard, Client client) {
        super(new GridBagLayout());
        this.coordinate = coord;
        this.client = client;
        this.chessBoard = chessBoard;
        this.boardPanel = boardPanel;

        // Create and add the icon label
        pieceIcon = new JLabel();
        pieceIcon.setHorizontalAlignment(JLabel.CENTER);
        pieceIcon.setVerticalAlignment(JLabel.CENTER);

        this.add(pieceIcon);

        // Set tile preferred size (initially 60x60, but will scale)
        setPreferredSize(new Dimension(BOARD_Configurations.TILE_SIZE, BOARD_Configurations.TILE_SIZE));

        // Assign colors and piece icon
        assignTileColor(chessBoard);
        assignTilePieceIcon(chessBoard);

        // Handle resize to rescale icon
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                assignTilePieceIcon(chessBoard); // Re-assign scaled icon on resize
            }
        });

        // Mouse handling for click/move and drag and drop logic
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Only handle clicks if not dragging and if it was a simple click (not a drag)
                if (!isDragging) {
                    handleMouseClick(e, chessBoard);
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e, chessBoard);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                handleMouseReleased(e, chessBoard);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        
        // Add mouse motion listener for dragging
        addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                handleMouseDragged(e, chessBoard);
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                // Not needed for drag and drop
            }
        });
        
        validate();
    }
    
    /**
     * Handle mouse click events (existing click-based functionality)
     */
    private void handleMouseClick(MouseEvent e, Board chessBoard) {
        System.out.println("DEBUG: TilePanel clicked at " + coordinate + ", client: " + (client != null ? "present" : "null"));
        System.out.println("DEBUG: Current player team: " + chessBoard.getCurrentPlayer().getTeam());
        
        // Validate turn using shared logic
        boolean isAIMode = (client == null);
        Team playerTeam;
        if (isAIMode) {
            // In AI mode, get player team from table
            Table table = boardPanel.getTable();
            playerTeam = (table != null && table.humanTeam != null) ? table.humanTeam : Team.WHITE;
        } else {
            playerTeam = client.getTeam();
        }
        boolean isPaired = isAIMode || client.isPaired;
        
        System.out.println("DEBUG: Turn validation - playerTeam: " + playerTeam + ", currentPlayer: " + chessBoard.getCurrentPlayer().getTeam() + ", isPaired: " + isPaired + ", isAIMode: " + isAIMode);
        
        if (!GameLogic.isValidTurn(chessBoard, playerTeam, isAIMode, isPaired)) {
            System.out.println("DEBUG: Invalid turn - playerTeam=" + playerTeam + ", currentPlayer=" + chessBoard.getCurrentPlayer().getTeam() + ", isPaired=" + isPaired);
            return;
        }
        
        // Use the common move execution logic
        executeMoveLogic(chessBoard, isAIMode, coordinate);
        
        boardPanel.updateBoardGUI(chessBoard);
    }
    
    /**
     * Handle mouse pressed events for drag and drop
     */
    private void handleMousePressed(MouseEvent e, Board chessBoard) {
        // Store the press point for potential drag detection
        dragStartPoint = e.getPoint();
        
        Tile thisTile = chessBoard.getTile(coordinate);
        if (thisTile.hasPiece()) {
            // Validate turn before starting drag
            boolean isAIMode = (client == null);
            Team playerTeam;
            if (isAIMode) {
                Table table = boardPanel.getTable();
                playerTeam = (table != null && table.humanTeam != null) ? table.humanTeam : Team.WHITE;
            } else {
                playerTeam = client.getTeam();
            }
            boolean isPaired = isAIMode || client.isPaired;
            
            if (GameLogic.isValidTurn(chessBoard, playerTeam, isAIMode, isPaired) &&
                thisTile.getPiece().getTeam() == chessBoard.getCurrentPlayer().getTeam()) {
                
                // Store the potential drag tile but don't start dragging yet
                draggedTile = thisTile;
                
                System.out.println("DEBUG: Mouse pressed on valid piece at " + coordinate);
            }
        }
    }
    
    /**
     * Handle mouse dragged events
     */
    private void handleMouseDragged(MouseEvent e, Board chessBoard) {
        if (!isDragging && draggedTile != null && dragStartPoint != null) {
            // Check if we've moved far enough to start a drag
            int dx = e.getX() - dragStartPoint.x;
            int dy = e.getY() - dragStartPoint.y;
            int distance = (int) Math.sqrt(dx * dx + dy * dy);
            
            if (distance > 5) { // Minimum drag distance threshold
                // Safety check: ensure no other drag is in progress
                if (draggedPieceIcon != null) {
                    System.out.println("WARNING: Attempting to start drag while another is in progress, cleaning up first");
                    forceCleanupDragState();
                    return;
                }
                // Start the drag operation
                isDragging = true;
                
                // Set this tile as chosen
                chessBoard.setChosenTile(draggedTile);
                
                // Create visual feedback
                createDraggedPieceIcon();
                
                // Start safety timeout timer (5 seconds to auto-cleanup)
                startDragTimeoutTimer();
                
                // Update board display
                boardPanel.updateBoardGUI(chessBoard);
                
                System.out.println("DEBUG: Started dragging piece at " + coordinate);
            }
        }
        
        if (isDragging && draggedPieceIcon != null) {
            // Update the floating piece position to follow the mouse cursor
            javax.swing.JRootPane rootPane = SwingUtilities.getRootPane(this);
            if (rootPane != null) {
                javax.swing.JComponent glassPane = (javax.swing.JComponent) rootPane.getGlassPane();
                if (glassPane != null && draggedPieceIcon.getParent() == glassPane) {
                    // Convert mouse position to glass pane coordinates
                    Point currentPoint = SwingUtilities.convertPoint(this, e.getPoint(), glassPane);
                    int size = draggedPieceIcon.getWidth();
                    
                    // Center the piece on the cursor
                    int x = currentPoint.x - size/2;
                    int y = currentPoint.y - size/2;
                    
                    // Keep the piece within the glass pane bounds
                    x = Math.max(0, Math.min(x, glassPane.getWidth() - size));
                    y = Math.max(0, Math.min(y, glassPane.getHeight() - size));
                    
                    draggedPieceIcon.setLocation(x, y);
                    
                    // Force immediate repaint for smooth movement
                    glassPane.repaint();
                    
                    // Debug: Track piece movement (remove in production)
                    // System.out.println("DEBUG: Moving floating piece to " + x + ", " + y);
                }
            }
        }
    }
    
    /**
     * Handle mouse released events for drag and drop
     */
    private void handleMouseReleased(MouseEvent e, Board chessBoard) {
        try {
            if (isDragging) {
                // This was a drag operation
                System.out.println("DEBUG: Processing drag release");
                
                // Store target information before cleanup
                Point releasePoint = SwingUtilities.convertPoint(this, e.getPoint(), boardPanel);
                TilePanel targetTile = findTileAt(releasePoint);
                
                // Always clean up drag state first to prevent visual artifacts
                cleanupDragState();
                
                if (targetTile != null && targetTile != this) {
                    Coordinate targetCoord = targetTile.getCoordinate();
                    System.out.println("DEBUG: Dropped piece at " + targetCoord);
                    
                    // Try to execute the move
                    try {
                        executeMoveLogic(chessBoard, (client == null), targetCoord);
                    } catch (Exception moveEx) {
                        System.err.println("ERROR: Failed to execute move: " + moveEx.getMessage());
                        moveEx.printStackTrace();
                        // Ensure chosen tile is cleared on error
                        chessBoard.setChosenTile(null);
                    }
                } else {
                    // Invalid drop - return piece to original position
                    System.out.println("DEBUG: Invalid drop, returning piece to original position");
                    chessBoard.setChosenTile(null);
                }
                
                // Force board update
                SwingUtilities.invokeLater(() -> {
                    boardPanel.updateBoardGUI(chessBoard);
                });
            } else {
                // This was not a drag operation, reset potential drag state
                draggedTile = null;
                dragStartPoint = null;
            }
        } catch (Exception ex) {
            System.err.println("ERROR: Exception in mouse released handler: " + ex.getMessage());
            ex.printStackTrace();
            // Force cleanup on any error
            forceCleanupDragState();
        }
    }
    
    /**
     * Create a visual representation of the piece being dragged
     */
    private void createDraggedPieceIcon() {
        if (draggedTile != null && draggedTile.hasPiece()) {
            // Hide the original piece
            pieceIcon.setVisible(false);
            
            // Create a floating label for the dragged piece
            draggedPieceIcon = new JLabel() {
                @Override
                protected void paintComponent(java.awt.Graphics g) {
                    // Add slight transparency for floating effect
                    java.awt.Graphics2D g2d = (java.awt.Graphics2D) g.create();
                    g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.85f));
                    super.paintComponent(g2d);
                    g2d.dispose();
                }
            };
            
            // Copy the piece icon
            ImageIcon pieceImage = BoardUtilities.getImageOfTeamPiece(
                draggedTile.getPiece().getTeam(),
                draggedTile.getPiece().getType()
            );
            
            // Scale the icon appropriately (slightly larger for better visibility)
            double scale = 0.9;
            int tileSize = this.getWidth() > 0 ? this.getWidth() : BOARD_Configurations.TILE_SIZE;
            int size = (int) (tileSize * scale);
            draggedPieceIcon.setIcon(getScaledIcon(pieceImage, size, size));
            draggedPieceIcon.setSize(size, size);
            
            // Find the root pane for layering
            javax.swing.JRootPane rootPane = SwingUtilities.getRootPane(this);
            if (rootPane != null) {
                // Get or create the glass pane for proper layering
                javax.swing.JComponent glassPane = (javax.swing.JComponent) rootPane.getGlassPane();
                
                // If glass pane doesn't exist or isn't a JComponent, create a new one
                if (glassPane == null || !(rootPane.getGlassPane() instanceof javax.swing.JComponent)) {
                    glassPane = new javax.swing.JPanel();
                    glassPane.setOpaque(false);
                    glassPane.setLayout(null); // Use absolute positioning
                    rootPane.setGlassPane(glassPane);
                }
                
                // Ensure proper layout and make visible
                glassPane.setLayout(null);
                glassPane.add(draggedPieceIcon);
                glassPane.setVisible(true);
                
                // Set initial position
                Point startPos = SwingUtilities.convertPoint(this, dragStartPoint, glassPane);
                draggedPieceIcon.setLocation(startPos.x - size/2, startPos.y - size/2);
                
                System.out.println("DEBUG: Created floating piece at position " + draggedPieceIcon.getLocation());
            } else {
                System.err.println("WARNING: Could not find root pane for floating piece");
            }
            
            // Change cursor to show dragging state
            this.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.MOVE_CURSOR));
            
            // Repaint
            this.repaint();
        }
    }
    
    /**
     * Find the tile panel at the given point
     */
    private TilePanel findTileAt(Point point) {
        try {
            // First try to find using component at point (more reliable)
            Component comp = boardPanel.getComponentAt(point);
            if (comp instanceof TilePanel) {
                return (TilePanel) comp;
            }
            
            // Fallback to bounds checking
            for (int i = 0; i < BOARD_Configurations.ROW_COUNT; i++) {
                for (int j = 0; j < BOARD_Configurations.ROW_TILE_COUNT; j++) {
                    TilePanel tile = boardPanel.getBoardTiles()[i][j];
                    if (tile != null && tile.getBounds().contains(point)) {
                        System.out.println("DEBUG: Found target tile at (" + i + ", " + j + ") for point " + point);
                        return tile;
                    }
                }
            }
            
            System.out.println("DEBUG: No tile found at point " + point);
            return null;
        } catch (Exception ex) {
            System.err.println("ERROR: Exception in findTileAt: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Clean up drag and drop state
     */
    private void cleanupDragState() {
        System.out.println("DEBUG: Cleaning up drag state");
        
        // Stop timeout timer
        stopDragTimeoutTimer();
        
        isDragging = false;
        dragStartPoint = null;
        draggedTile = null;
        
        // Remove the floating piece icon from glass pane
        if (draggedPieceIcon != null) {
            try {
                javax.swing.JRootPane rootPane = SwingUtilities.getRootPane(this);
                if (rootPane != null) {
                    javax.swing.JComponent glassPane = (javax.swing.JComponent) rootPane.getGlassPane();
                    if (glassPane != null) {
                        // Remove the component if it's there
                        if (draggedPieceIcon.getParent() == glassPane) {
                            glassPane.remove(draggedPieceIcon);
                        }
                        // Always repaint the glass pane
                        glassPane.repaint();
                        // Hide glass pane if it's empty
                        if (glassPane.getComponentCount() == 0) {
                            glassPane.setVisible(false);
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("ERROR: Exception during floating piece cleanup: " + ex.getMessage());
            }
            draggedPieceIcon = null;
        }
        
        // Restore the original piece icon visibility
        pieceIcon.setVisible(true);
        
        // Reset cursor to default
        this.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
        
        // Refresh the piece icon to ensure it's current
        try {
            if (chessBoard != null) {
                assignTilePieceIcon(chessBoard);
            }
        } catch (Exception ex) {
            System.err.println("ERROR: Exception during piece icon assignment: " + ex.getMessage());
        }
        
        // Force repaint of this tile
        this.revalidate();
        this.repaint();
        
        System.out.println("DEBUG: Drag state cleanup completed");
    }
    
    /**
     * Force cleanup of drag state - used in error scenarios
     */
    private void forceCleanupDragState() {
        System.out.println("DEBUG: Force cleaning up drag state");
        
        // Stop timeout timer
        stopDragTimeoutTimer();
        
        // Reset all drag-related variables
        isDragging = false;
        dragStartPoint = null;
        draggedTile = null;
        
        // Force remove any floating piece icons from all possible glass panes
        try {
            javax.swing.JRootPane rootPane = SwingUtilities.getRootPane(this);
            if (rootPane != null) {
                javax.swing.JComponent glassPane = (javax.swing.JComponent) rootPane.getGlassPane();
                if (glassPane != null) {
                    // Remove all components (brute force cleanup)
                    glassPane.removeAll();
                    glassPane.setVisible(false);
                    glassPane.repaint();
                }
            }
        } catch (Exception ex) {
            System.err.println("ERROR: Exception during force cleanup: " + ex.getMessage());
        }
        
        // Reset floating piece reference
        draggedPieceIcon = null;
        
        // Restore piece visibility
        pieceIcon.setVisible(true);
        
        // Reset cursor
        this.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
        
        // Force refresh
        SwingUtilities.invokeLater(() -> {
            try {
                if (chessBoard != null) {
                    assignTilePieceIcon(chessBoard);
                }
                this.revalidate();
                this.repaint();
                if (boardPanel != null) {
                    boardPanel.updateBoardGUI(chessBoard);
                }
            } catch (Exception ex) {
                System.err.println("ERROR: Exception during force refresh: " + ex.getMessage());
            }
        });
        
        System.out.println("DEBUG: Force cleanup completed");
    }
    
    /**
     * Start the drag timeout timer to prevent stuck drags
     */
    private void startDragTimeoutTimer() {
        // Stop any existing timer
        if (dragTimeoutTimer != null) {
            dragTimeoutTimer.stop();
        }
        
        // Create new timeout timer (5 seconds)
        dragTimeoutTimer = new javax.swing.Timer(5000, e -> {
            System.out.println("WARNING: Drag timeout reached, forcing cleanup");
            forceCleanupDragState();
        });
        dragTimeoutTimer.setRepeats(false);
        dragTimeoutTimer.start();
    }
    
    /**
     * Stop the drag timeout timer
     */
    private void stopDragTimeoutTimer() {
        if (dragTimeoutTimer != null) {
            dragTimeoutTimer.stop();
            dragTimeoutTimer = null;
        }
    }
    
    /**
     * Common move execution logic for both click and drag operations
     */
    private void executeMoveLogic(Board chessBoard, boolean isAIMode, Coordinate targetCoord) {
        System.out.println("DEBUG: Turn validation passed - allowing move");
        
        // Additional multiplayer checks
        if (!isAIMode && client.isMovePending()) {
            System.out.println("DEBUG: Move already pending, blocking additional moves");
            return;
        }

        if (!chessBoard.hasChosenTile()) {
            // if there is no chosen piece, then make this piece chosen
            if (chessBoard.getTile(targetCoord).hasPiece()) {
                if (chessBoard.getCurrentPlayer().getTeam() != chessBoard.getTile(targetCoord).getPiece().getTeam()) {
                    return;
                }
            }
            chessBoard.setChosenTile(chessBoard.getTile(targetCoord));
        } else {
            Tile destinationTile = chessBoard.getTile(targetCoord); // if there is already a chosen piece then this tile will be destination place
            if (MoveUtilities.isValidMove(chessBoard, destinationTile)) {
                // Handle AI mode moves
                if (isAIMode) {
                    Tile chosenTile = chessBoard.getChosenTile();
                    Move move = null;
                    
                    // Handle en passant
                    if (chosenTile != null && chosenTile.hasPiece() && chosenTile.getPiece().getType() == PieceTypes.PAWN) {
                        for (Move m : chosenTile.getPiece().availableMoves(chessBoard, chosenTile.getCoordinate())) {
                            if (m.isEnPassantMove() && m.getDestinationTile().getCoordinate().equals(destinationTile.getCoordinate())) {
                                move = m;
                                break;
                            }
                        }
                    }
                    
                    // Handle castling
                    if (move == null && chosenTile != null && chosenTile.hasPiece() && chosenTile.getPiece().getType() == PieceTypes.KING) {
                        move = GameLogic.createCastlingMove(chessBoard, chosenTile.getCoordinate(), destinationTile.getCoordinate());
                    }
                    
                    // Handle regular moves and promotion
                    if (move == null) {
                        move = GameLogic.createPromotionMove(chessBoard, chosenTile.getCoordinate(), destinationTile.getCoordinate(), 
                                                           chessBoard.getCurrentPlayer().getTeam(), boardPanel, true);
                    }
                    
                    if (move != null) {
                        // Track captured pieces
                        Table table = boardPanel.getTable();
                        if (move.hasKilledPiece() && table != null && table.getBottomGameMenu() != null) {
                            table.getBottomGameMenu().addCapturedPiece(
                                move.getKilledPiece().toString(),
                                move.getKilledPiece().getPoints()
                            );
                        }
                        
                        // Execute human move
                        chessBoard.getCurrentPlayer().makeMove(chessBoard, move);
                        
                        // Track this as the last move
                        if (table != null) {
                            table.setLastMove(move);
                        }
                        
                        // Update board display
                        boardPanel.updateBoardGUI(chessBoard);
                        
                        // Switch to AI player
                        chessBoard.changeCurrentPlayer();
                        
                        // Check for game end before AI move
                        if (table != null && GameLogic.checkGameEnd(chessBoard, table, true)) {
                            chessBoard.setChosenTile(null);
                            return;
                        }
                        
                        // Trigger AI move
                        if (table != null) {
                            table.executeAIMoveIfNeeded();
                        }
                    }
                    
                    chessBoard.setChosenTile(null);
                    return;
                }
                
                // Handle multiplayer mode moves  
                Tile chosenTile = chessBoard.getChosenTile();
                Move move = null;
                
                // Check for en passant
                if (chosenTile != null && chosenTile.hasPiece() && chosenTile.getPiece().getType() == PieceTypes.PAWN) {
                    for (Move m : chosenTile.getPiece().availableMoves(chessBoard, chosenTile.getCoordinate())) {
                        if (m.isEnPassantMove() && m.getDestinationTile().getCoordinate().equals(destinationTile.getCoordinate())) {
                            move = m;
                            break;
                        }
                    }
                }
                
                // Check for castling
                if (move == null && chosenTile != null && chosenTile.hasPiece() && chosenTile.getPiece().getType() == PieceTypes.KING) {
                    move = GameLogic.createCastlingMove(chessBoard, chosenTile.getCoordinate(), destinationTile.getCoordinate());
                }
                
                // Handle regular moves and promotion
                if (move == null) {
                    move = GameLogic.createPromotionMove(chessBoard, chosenTile.getCoordinate(), destinationTile.getCoordinate(), 
                                                       chessBoard.getCurrentPlayer().getTeam(), boardPanel, false);
                }
                
                if (move != null) {
                    // Send move to server or execute locally based on connection status
                    executeMove(chessBoard, move, isAIMode);
                }
                
                return;
            } else {
                // If destination tile has a piece and it's the current player's piece, select it instead
                if (destinationTile.hasPiece()) {
                    if (chessBoard.getCurrentPlayer().getTeam() == destinationTile.getPiece().getTeam()) {
                        chessBoard.setChosenTile(destinationTile);
                    }
                }
            }
        }
        
        // Check for check state and send notification if in multiplayer mode
        if (!isAIMode && client != null) {
            if (MoveUtilities.controlCheckState(chessBoard, Team.BLACK)) {
                Message msg = new Message(Message.MessageTypes.CHECK);
                msg.content = (Object) Team.BLACK;
                client.Send(msg);
            } else if (MoveUtilities.controlCheckState(chessBoard, Team.WHITE)) {
                Message msg = new Message(Message.MessageTypes.CHECK);
                msg.content = (Object) Team.WHITE;
                client.Send(msg);
            }
        }
    }
    
    /**
     * Execute a move either locally (AI mode) or send to server (multiplayer mode)
     */
    private void executeMove(Board chessBoard, Move move, boolean isAIMode) {
        if (isAIMode) {
            // Execute move locally in AI mode
            chessBoard.getCurrentPlayer().makeMove(chessBoard, move);
            
            // Track this as the last move
            Table table = boardPanel.getTable();
            if (table != null) {
                table.setLastMove(move);
            }
            
            // Update board display
            boardPanel.updateBoardGUI(chessBoard);
            
            // Switch to AI player
            chessBoard.changeCurrentPlayer();
            
            // Check for game end before AI move
            if (table != null && GameLogic.checkGameEnd(chessBoard, table, true)) {
                chessBoard.setChosenTile(null);
                return;
            }
            
            // Trigger AI move
            if (table != null) {
                table.executeAIMoveIfNeeded();
            }
            
            chessBoard.setChosenTile(null);
        } else {
            // Send move to server in multiplayer mode
            sendMoveToServer(move, chessBoard);
        }
    }
    
    /**
     * Send move to server for multiplayer mode
     */
    private void sendMoveToServer(Move move, Board chessBoard) {
        Message msg = new Message(Message.MessageTypes.MOVE);
        MovementMessage movement = new MovementMessage();
        movement.currentCoordinate = move.getCurrentTile().getCoordinate();
        movement.destinationCoordinate = move.getDestinationTile().getCoordinate();
        
        // Set special move flags
        if (move.isEnPassantMove()) {
            movement.isEnPassant = true;
            movement.enPassantCapturedPawnCoordinate = move.getEnPassantCapturedTile().getCoordinate();
        }
        if (move.isCastlingMove()) {
            movement.isCastling = true;
            movement.rookStartCoordinate = move.getRookStartTile().getCoordinate();
            movement.rookEndCoordinate = move.getRookEndTile().getCoordinate();
        }
        if (move.hasKilledPiece()) {
            movement.isPieceKilled = true;
        }
        if (move.isPromotionMove()) {
            movement.isPromotion = true;
            movement.promotionPieceType = move.getPromotionPieceType().toString();
        }
        
        msg.content = (Object) movement;
        
        // Check if client is properly connected to server
        if (client.socket == null || client.socket.isClosed()) {
            System.out.println("DEBUG: Server not connected - executing move in offline mode");
            // Execute move locally in offline mode
            chessBoard.getCurrentPlayer().makeMove(chessBoard, move);
            boardPanel.updateBoardGUI(chessBoard);
            chessBoard.changeCurrentPlayer();
            
            // Update turn display for offline mode
            if (client.game.getBottomGameMenu() != null) {
                if (chessBoard.getCurrentPlayer().getTeam() == client.getTeam()) {
                    client.game.getBottomGameMenu().getTurnLBL().setText("Your Turn (Offline Mode)");
                    client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.GREEN);
                } else {
                    client.game.getBottomGameMenu().getTurnLBL().setText("Opponent's Turn (Offline Mode)");
                    client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.BLUE);
                }
            }
            
            chessBoard.setChosenTile(null);
            return;
        }
        
        // Set move pending state BEFORE sending
        client.setMovePending(true);
        
        // Start timeout timer (5 seconds)
        client.startMoveTimeout(5000);
        
        // Send move to server for validation first
        client.Send(msg);
        
        // Immediately update UI to show waiting state
        client.game.getBottomGameMenu().getTurnLBL().setText("Move Sent - Waiting for Server");
        client.game.getBottomGameMenu().getTurnLBL().setForeground(Color.ORANGE);
        
        // Clear the chosen tile to prevent further moves
        chessBoard.setChosenTile(null);
        boardPanel.updateBoardGUI(chessBoard);
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    // ✅ MODIFIED: Dynamically scale and assign image to match tile size
    public void assignTilePieceIcon(Board board) {
        Tile thisTile = board.getTile(this.coordinate);
        if (thisTile == null) {
            System.out.println("Tile is null");
            return;
        }

        if (thisTile.hasPiece()) {
            ImageIcon rawIcon = BoardUtilities.getImageOfTeamPiece(
                    thisTile.getPiece().getTeam(),
                    thisTile.getPiece().getType()
            );
            // Scale to current tile size
            double scale = 0.7;
            int w = this.getWidth() > 0 ? this.getWidth() : BOARD_Configurations.TILE_SIZE;
            int h = this.getHeight() > 0 ? this.getHeight() : BOARD_Configurations.TILE_SIZE;
            int size = (int) (Math.min(w, h) * scale) ;

            pieceIcon.setIcon(getScaledIcon(rawIcon, size, size));
        }
        else if (!thisTile.hasPiece()) {
            pieceIcon.setIcon(null);
        }
        pieceIcon.validate();
    }

    // ✅ NEW: Helper method to scale icons
    private ImageIcon getScaledIcon(ImageIcon icon, int width, int height) {
        return new ImageIcon(icon.getImage().getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH));
    }

    public void assignTileColor(Board board) {

        // Default coloring
         if (this.coordinate.getX() % 2 == 0 && this.coordinate.getY() % 2 == 0) {
            this.setBackground(GUI_Configurations.lightColor);
        } else if (this.coordinate.getX() % 2 == 0 && this.coordinate.getY() % 2 == 1) {
            this.setBackground(GUI_Configurations.darkColor);
        } else if (this.coordinate.getX() % 2 == 1 && this.coordinate.getY() % 2 == 0) {
            this.setBackground(GUI_Configurations.darkColor);
        } else if (this.coordinate.getX() % 2 == 1 && this.coordinate.getY() % 2 == 1) {
            this.setBackground(GUI_Configurations.lightColor);
        }

        // Highlight last move (from and to squares) with improved colors
        Table table = boardPanel.getTable();
        boolean isHighlighted = false;
        if (table != null && table.getLastMove() != null) {
            Move lastMove = table.getLastMove();
            Coordinate fromCoord = lastMove.getCurrentTile().getCoordinate();
            Coordinate toCoord = lastMove.getDestinationTile().getCoordinate();
            
            if (this.coordinate.equals(fromCoord)) {
                // Light green for the square the piece moved from
                this.setBackground(new Color(144, 238, 144)); // Light green
                this.setBorder(BorderFactory.createLineBorder(new Color(34, 139, 34), 3)); // Forest green border
                isHighlighted = true;
            } else if (this.coordinate.equals(toCoord)) {
                // Darker green for the square the piece moved to (destination)
                this.setBackground(new Color(50, 205, 50)); // Lime green
                this.setBorder(BorderFactory.createLineBorder(new Color(0, 100, 0), 3)); // Dark green border
                isHighlighted = true;
            }
        }
        
        // Highlight chosen tile (higher priority than last move)
        if (board.hasChosenTile()) {
            if (this.coordinate.equals(board.getChosenTile().getCoordinate())) {
                this.setBackground(Color.GREEN);
            }
        }

        // Highlight checked king's square (highest priority)
        boolean blackInCheck = MoveUtilities.controlCheckState(board, Team.BLACK);
        boolean whiteInCheck = MoveUtilities.controlCheckState(board, Team.WHITE);
        Coordinate blackKingCoord = board.getCoordOfGivenTeamPiece(Team.BLACK, PieceTypes.KING);
        Coordinate whiteKingCoord = board.getCoordOfGivenTeamPiece(Team.WHITE, PieceTypes.KING);
        if (blackInCheck && blackKingCoord != null && this.coordinate.equals(blackKingCoord)) {
            this.setBackground(GUI_Configurations.checkRedColor);
        } else if (whiteInCheck && whiteKingCoord != null && this.coordinate.equals(whiteKingCoord)) {
            this.setBackground(GUI_Configurations.checkRedColor);
        }

        // Apply default border only if not already highlighted with a special border
        if (!isHighlighted) {
            this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        // Ensure square size based on current width or height
        int size = Math.min(getWidth(), getHeight());
        if (size == 0) {
            size = BOARD_Configurations.TILE_SIZE; // fallback default
        }
        return new Dimension(size, size);
    }

    /**
     * Helper method to detect if we're in AI mode
     * @return true if this is AI mode (client is null), false if multiplayer
     */
    private boolean isAIMode() {
        return client == null;
    }
    
    /**
     * Check for checkmate or stalemate and handle game ending
     */
    private void checkGameEnding(Board chessBoard, Client client) {
        String gameState = MoveUtilities.getGameState(chessBoard, chessBoard.getCurrentPlayer().getTeam());
        if (gameState != null) {
            if (gameState.equals("CHECKMATE")) {
                Team winnerTeam = (chessBoard.getCurrentPlayer().getTeam() == Team.BLACK) ? Team.WHITE : Team.BLACK;
                JOptionPane.showMessageDialog(boardPanel, 
                    "Checkmate! " + winnerTeam.toString() + " wins!", 
                    "Game Over", 
                    JOptionPane.INFORMATION_MESSAGE);
                Message endMessage = new Message(Message.MessageTypes.END);
                endMessage.content = winnerTeam.toString();
                client.Send(endMessage);
            } else if (gameState.equals("STALEMATE")) {
                JOptionPane.showMessageDialog(boardPanel, 
                    "Stalemate! The game is a draw.", 
                    "Game Over", 
                    JOptionPane.INFORMATION_MESSAGE);
                Message endMessage = new Message(Message.MessageTypes.END);
                endMessage.content = "DRAW";
                client.Send(endMessage);
            }
        }
    }
}
