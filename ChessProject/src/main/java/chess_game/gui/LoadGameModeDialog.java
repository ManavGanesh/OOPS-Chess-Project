package chess_game.gui;

import Messages.GameState;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog for choosing between offline and multiplayer mode when loading a saved game
 */
public class LoadGameModeDialog extends JDialog {
    
    private JButton offlineButton;
    private JButton multiplayerButton;
    private JButton cancelButton;
    private JLabel infoLabel;
    private JLabel restrictionLabel;
    
    private GameState gameState;
    private boolean offlineModeSelected = false;
    private boolean multiplayerModeSelected = false;
    
    public LoadGameModeDialog(JFrame parent, GameState gameState) {
        super(parent, "Load Game Mode", true);
        this.gameState = gameState;
        initComponents();
        setupEventHandlers();
    }
    
    private void initComponents() {
        setSize(450, 250);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Create components
        JLabel titleLabel = new JLabel("Choose how to load this game:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Game info label
        infoLabel = new JLabel();
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        updateGameInfo();
        
        // Restriction label
        restrictionLabel = new JLabel();
        restrictionLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        restrictionLabel.setForeground(new Color(200, 0, 0));
        restrictionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        updateRestrictionInfo();
        
        // Buttons
        offlineButton = new JButton(gameState.isAIGame() ? "Play with AI" : "Load in Offline Mode");
        multiplayerButton = new JButton("Load in Multiplayer Mode");
        cancelButton = new JButton("Cancel");
        
        // Style buttons
        JButton[] buttons = {offlineButton, multiplayerButton, cancelButton};
        for (JButton btn : buttons) {
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setPreferredSize(new Dimension(180, 35));
        }
        
        offlineButton.setBackground(new Color(0, 150, 0));
        offlineButton.setForeground(Color.WHITE);
        multiplayerButton.setBackground(new Color(70, 130, 180));
        multiplayerButton.setForeground(Color.WHITE);
        cancelButton.setBackground(Color.GRAY);
        cancelButton.setForeground(Color.WHITE);
        
        // Layout
        setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(titleLabel, BorderLayout.NORTH);
        topPanel.add(infoLabel, BorderLayout.CENTER);
        topPanel.add(restrictionLabel, BorderLayout.SOUTH);
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        // Only add the relevant button(s)
        if (gameState.isAIGame()) {
            buttonPanel.add(offlineButton);
        } else {
            buttonPanel.add(multiplayerButton);
        }
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(cancelButton);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 15, 15));
        
        add(topPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void updateGameInfo() {
        String gameType = gameState.isAIGame() ? "AI Game" : "Multiplayer Game";
        infoLabel.setText("<html><b>Game:</b> " + gameState.getSaveName() + 
                         "<br><b>Type:</b> " + gameType +
                         "<br><b>Players:</b> " + gameState.getPlayer1Name() + " vs " + gameState.getPlayer2Name() +
                         "<br><b>Current Turn:</b> " + gameState.getCurrentPlayerTeam() + "</html>");
    }
    
    private void updateRestrictionInfo() {
        if (gameState.isAIGame()) {
            restrictionLabel.setText("⚠ AI games can only be played offline");
        } else {
            restrictionLabel.setText("⚠ Multiplayer games require an opponent to continue");
        }
    }
    
    private void setupEventHandlers() {
        offlineButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                offlineModeSelected = true;
                dispose();
            }
        });
        
        multiplayerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                multiplayerModeSelected = true;
                dispose();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
    
    /**
     * Check if offline mode was selected
     * @return true if offline mode was selected, false otherwise
     */
    public boolean isOfflineModeSelected() {
        return offlineModeSelected;
    }
    
    /**
     * Check if multiplayer mode was selected
     * @return true if multiplayer mode was selected, false otherwise
     */
    public boolean isMultiplayerModeSelected() {
        return multiplayerModeSelected;
    }
    
    /**
     * Get the game state being loaded
     * @return The GameState object
     */
    public GameState getGameState() {
        return gameState;
    }
} 