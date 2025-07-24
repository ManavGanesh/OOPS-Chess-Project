package chess_game.gui;

import Messages.GameState;
import chess_game.Utilities.GameStateManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Dialog for selecting and loading saved chess games
 */
public class LoadGameDialog extends JDialog {
    
    private JList<String> savesList;
    private DefaultListModel<String> savesListModel;
    private JButton loadButton;
    private JButton deleteButton;
    private JButton cancelButton;
    private JLabel statusLabel;
    
    private GameState selectedGameState;
    private boolean gameLoaded = false;
    private String playerUniqueUsername;
    
    public LoadGameDialog(JFrame parent) {
        this(parent, null);
    }
    
    public LoadGameDialog(JFrame parent, String playerUniqueUsername) {
        super(parent, "Load Game", true);
        this.playerUniqueUsername = playerUniqueUsername;
        initComponents();
        loadSavedGames();
        setupEventHandlers();
    }
    
    private void initComponents() {
        setSize(500, 400);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Create components
        JLabel titleLabel = new JLabel("Select a saved game to load:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        savesListModel = new DefaultListModel<>();
        savesList = new JList<>(savesListModel);
        savesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        savesList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(savesList);
        scrollPane.setPreferredSize(new Dimension(450, 200));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Saved Games"));
        
        // Buttons
        loadButton = new JButton("Load Game");
        deleteButton = new JButton("Delete Save");
        cancelButton = new JButton("Cancel");
        
        JButton refreshButton = new JButton("Refresh");
        
        // Style buttons
        JButton[] buttons = {loadButton, deleteButton, cancelButton, refreshButton};
        for (JButton btn : buttons) {
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setPreferredSize(new Dimension(120, 30));
        }
        
        loadButton.setBackground(new Color(0, 150, 0));
        loadButton.setForeground(Color.WHITE);
        deleteButton.setBackground(new Color(200, 50, 50));
        deleteButton.setForeground(Color.WHITE);
        cancelButton.setBackground(Color.GRAY);
        cancelButton.setForeground(Color.WHITE);
        refreshButton.setBackground(new Color(70, 130, 180));
        refreshButton.setForeground(Color.WHITE);
        
        // Initially disable load and delete buttons
        loadButton.setEnabled(false);
        deleteButton.setEnabled(false);
        
        // Status label
        statusLabel = new JLabel("Select a game from the list above.");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setForeground(Color.GRAY);
        
        // Layout
        setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(refreshButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(loadButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(cancelButton);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        
        // Button actions
        loadButton.addActionListener(e -> loadSelectedGame());
        deleteButton.addActionListener(e -> deleteSelectedGame());
        cancelButton.addActionListener(e -> dispose());
        refreshButton.addActionListener(e -> {
            loadSavedGames();
            statusLabel.setText("Game list refreshed.");
        });
    }
    
    private void setupEventHandlers() {
        // List selection handler
        savesList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = savesList.getSelectedIndex() != -1;
                loadButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
                
                if (hasSelection) {
                    statusLabel.setText("Click 'Load Game' to load the selected save.");
                } else {
                    statusLabel.setText("Select a game from the list above.");
                }
            }
        });
        
        // Double-click to load
        savesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && savesList.getSelectedIndex() != -1) {
                    loadSelectedGame();
                }
            }
        });
    }
    
    private void loadSavedGames() {
        savesListModel.clear();
        List<String> saveNames;
        
        if (playerUniqueUsername != null) {
            // Load only games accessible by this player
            saveNames = GameStateManager.getPlayerSaveNames(playerUniqueUsername);
        } else {
            // Load all games (backward compatibility)
            saveNames = GameStateManager.getSaveNames();
        }
        
        if (saveNames.isEmpty()) {
            savesListModel.addElement("No saved games found.");
            savesList.setEnabled(false);
            statusLabel.setText("No saved games found. Save a game first to see it here.");
        } else {
            for (String saveName : saveNames) {
                savesListModel.addElement(saveName);
            }
            savesList.setEnabled(true);
            statusLabel.setText(saveNames.size() + " saved game(s) found.");
        }
    }
    
    private void loadSelectedGame() {
        String selected = savesList.getSelectedValue();
        if (selected == null || selected.equals("No saved games found.")) {
            return;
        }
        
        // Extract the save name from the display string
        // Format is "SaveName (Player1 vs Player2) [Type] - Date"
        String saveName = selected.split(" \\(")[0];
        
        try {
            statusLabel.setText("Loading game...");
            selectedGameState = GameStateManager.loadGame(saveName);
            
            if (selectedGameState != null) {
                // Show mode selection dialog
                LoadGameModeDialog modeDialog = new LoadGameModeDialog((JFrame) getParent(), selectedGameState);
                modeDialog.setVisible(true);
                
                if (modeDialog.isOfflineModeSelected() || modeDialog.isMultiplayerModeSelected()) {
                    gameLoaded = true;
                    statusLabel.setText("Game loaded successfully!");
                    
                    // Close dialog without showing additional popup
                    SwingUtilities.invokeLater(() -> {
                        dispose();
                    });
                } else {
                    // User cancelled mode selection
                    selectedGameState = null;
                    statusLabel.setText("Load cancelled.");
                }
            } else {
                statusLabel.setText("Failed to load game. File may be corrupted.");
                JOptionPane.showMessageDialog(this, 
                    "Failed to load the selected game.\nThe save file may be corrupted or incompatible.", 
                    "Load Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            statusLabel.setText("Error loading game.");
            JOptionPane.showMessageDialog(this, 
                "Error loading game: " + e.getMessage(), 
                "Load Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void deleteSelectedGame() {
        String selected = savesList.getSelectedValue();
        if (selected == null || selected.equals("No saved games found.")) {
            return;
        }
        
        // Extract the save name from the display string
        String saveName = selected.split(" \\(")[0];
        
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete the save '" + saveName + "'?\nThis action cannot be undone.",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (result == JOptionPane.YES_OPTION) {
            if (GameStateManager.deleteSave(saveName)) {
                statusLabel.setText("Save deleted successfully.");
                loadSavedGames(); // Refresh the list
            } else {
                statusLabel.setText("Failed to delete save.");
                JOptionPane.showMessageDialog(this,
                    "Failed to delete the save. Please try again.",
                    "Delete Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Check if a game was successfully loaded
     * @return true if a game was loaded, false otherwise
     */
    public boolean isGameLoaded() {
        return gameLoaded;
    }
    
    /**
     * Get the loaded game state
     * @return The loaded GameState, or null if no game was loaded
     */
    public GameState getLoadedGameState() {
        return selectedGameState;
    }
}
