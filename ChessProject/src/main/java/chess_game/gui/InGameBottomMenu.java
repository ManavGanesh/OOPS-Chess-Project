package chess_game.gui;

import javax.swing.*;
import java.awt.*;
import chess_game.Pieces.Team;

public class InGameBottomMenu extends javax.swing.JPanel {

    public DefaultListModel<String> blackCapturedPiecesListModel;
    public DefaultListModel<String> whiteCapturedPiecesListModel;
    public DefaultListModel<String> chatListModel;
    
    // Score tracking
    private int whiteScore = 0;
    private int blackScore = 0;
    private javax.swing.JLabel blackScoreLabel;
    private javax.swing.JLabel whiteScoreLabel;
    
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel playersColorLBL;
    private javax.swing.JLabel whiteCapturedLBL;
    private javax.swing.JScrollPane rightScrollPane;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JList<String> blackCapturedPiecesLIST;
    private javax.swing.JList<String> whiteCapturedPiecesLIST;
    private javax.swing.JLabel turnLBL;
    private javax.swing.JLabel playerNameLBL;
    private javax.swing.JLabel opponentNameLBL;
    private JPanel leftNamePanel;
    private JPanel rightNamePanel;
    private Team playerTeam;
    
    // Chat components
    private javax.swing.JScrollPane chatScrollPane;
    private javax.swing.JList<String> chatLIST;
    private javax.swing.JTextField chatInputTXT;
    private javax.swing.JButton sendBTN;
    private javax.swing.JLabel chatLBL;
    
    // Quit game button
    private javax.swing.JButton quitGameBTN;
    
    // Save game button
    private javax.swing.JButton saveGameBTN;
    
    // Tutor system components
    private javax.swing.JButton hintBTN;
    private int hintsRemaining = -1; // Unlimited hints
    private javax.swing.JLabel hintCounterLBL;
    private boolean isAIMode;
    
    // Timer components (for multiplayer mode)
    private javax.swing.JLabel whiteTimerLabel;
    private javax.swing.JLabel blackTimerLabel;
    private javax.swing.Timer whiteTimer;
    private javax.swing.Timer blackTimer;
    private int whiteTimeRemaining = 0; // in seconds
    private int blackTimeRemaining = 0; // in seconds
    private boolean whiteTimerActive = false;
    private boolean blackTimerActive = false;
    private boolean gameTimersPaused = false;
    private boolean timersInitialized = false;
    private boolean isWhiteTurn = true;

    public InGameBottomMenu(Team team) {
        this(team, false); // Default to multiplayer mode with chat
    }
    
    public InGameBottomMenu(Team team, boolean isAIMode) {
        this.playerTeam = team;
        this.isAIMode = isAIMode;
        blackCapturedPiecesListModel = new DefaultListModel<>();
        whiteCapturedPiecesListModel = new DefaultListModel<>();
        
        if (isAIMode) {
            initComponents(true); // AI mode
        } else {
            initComponents(false); // Multiplayer mode
            chatListModel = new DefaultListModel<>();
            chatLIST.setModel(chatListModel);
        }
        
        blackCapturedPiecesLIST.setModel(blackCapturedPiecesListModel);
        whiteCapturedPiecesLIST.setModel(whiteCapturedPiecesListModel);
        
        updateNamePanelColors();
        updateScoreDisplay(); // Initialize score display
    }

    private void updateNamePanelColors() {
        // For consistency, always use these colors regardless of player team:
        // Left panel (Black captured pieces) - White background with black text
        // Right panel (White captured pieces) - Black background with white text
        
        // Left player name panel
        leftNamePanel.setBackground(Color.WHITE);
        playerNameLBL.setForeground(Color.BLACK);
        
        // Right player name panel  
        rightNamePanel.setBackground(Color.BLACK);
        opponentNameLBL.setForeground(Color.WHITE);
        
        // Black captured pieces panel (left side) - White background
        jScrollPane2.setBackground(Color.WHITE);
        jScrollPane2.getViewport().setBackground(Color.WHITE);
        blackCapturedPiecesLIST.setBackground(Color.WHITE);
        blackCapturedPiecesLIST.setForeground(Color.BLACK);
        blackCapturedPiecesLIST.setSelectionBackground(Color.LIGHT_GRAY);
        blackCapturedPiecesLIST.setSelectionForeground(Color.BLACK);
        jLabel2.setBackground(Color.WHITE);
        jLabel2.setForeground(Color.BLACK);
        
        // White captured pieces panel (right side) - Black background
        rightScrollPane.setBackground(Color.BLACK);
        rightScrollPane.getViewport().setBackground(Color.BLACK);
        whiteCapturedPiecesLIST.setBackground(Color.BLACK);
        whiteCapturedPiecesLIST.setForeground(Color.WHITE);
        whiteCapturedPiecesLIST.setSelectionBackground(Color.DARK_GRAY);
        whiteCapturedPiecesLIST.setSelectionForeground(Color.WHITE);
        whiteCapturedLBL.setBackground(Color.BLACK);
        whiteCapturedLBL.setForeground(Color.WHITE);
        
        // Force repaint to ensure colors are applied
        SwingUtilities.invokeLater(() -> {
            jScrollPane2.repaint();
            rightScrollPane.repaint();
            blackCapturedPiecesLIST.repaint();
            whiteCapturedPiecesLIST.repaint();
        });
    }

    public JLabel getPlayersColorLBL() {
        return playersColorLBL;
    }

    public JList<String> getBlackCapturedPiecesLIST() {
        return blackCapturedPiecesLIST;
    }

    public JList<String> getWhiteCapturedPiecesLIST() {
        return whiteCapturedPiecesLIST;
    }

    public JLabel getTurnLBL() {
        return turnLBL;
    }

    public JLabel getPlayerNameLBL() {
        return playerNameLBL;
    }

    public JLabel getOpponentNameLBL() {
        return opponentNameLBL;
    }

    public JTextField getChatInputTXT() {
        return chatInputTXT;
    }

    public JButton getSendBTN() {
        return sendBTN;
    }
    
    public JButton getQuitGameBTN() {
        return quitGameBTN;
    }

    public JButton getSaveGameBTN() {
        return saveGameBTN;
    }
    
    public JButton getHintBTN() {
        return hintBTN;
    }
    
    public int getHintsRemaining() {
        return hintsRemaining;
    }
    
    public void useHint() {
        // No limit on hints anymore - just update display
        updateHintDisplay();
    }
    
    public void resetHints() {
        // No need to reset since hints are unlimited
        updateHintDisplay();
    }
    
    private void updateHintDisplay() {
        hintCounterLBL.setText(""); // Hide the counter label
        hintCounterLBL.setVisible(false); // Make it invisible
        hintBTN.setEnabled(true); // Always enabled
        hintBTN.setBackground(new Color(40, 167, 69)); // Always green
        hintBTN.setForeground(Color.WHITE);
    }

    public void setPlayerName(String playerName) {
        playerNameLBL.setText("You");
    }

public void setOpponentName(String opponentName) {
    if (opponentName != null && !opponentName.isEmpty()) {
        String newText = opponentName;
        if (!newText.equals(opponentNameLBL.getText())) { // Update only if different
            opponentNameLBL.setText("OPPONENT");
            System.out.println("DEBUG: InGameBottomMenu opponent name updated to: " + opponentName);
            // Force a repaint
            SwingUtilities.invokeLater(() -> {
                opponentNameLBL.repaint();
                rightNamePanel.repaint();
            });
        }
    } else {
        System.out.println("DEBUG: WARNING - Attempted to set null or empty opponent name in InGameBottomMenu");
    }
}

    public void addChatMessage(String message) {
        chatListModel.addElement(message);
        // Auto-scroll to bottom
        SwingUtilities.invokeLater(() -> {
            int lastIndex = chatLIST.getModel().getSize() - 1;
            if (lastIndex >= 0) {
                chatLIST.ensureIndexIsVisible(lastIndex);
            }
        });
    }

    private void initComponents() {
        initComponents(false); // Default to multiplayer mode with chat
    }
    
    private void initComponents(boolean isAIMode) {
        this.setBackground(new Color(223, 238, 222)); // Light green background

        playersColorLBL = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        rightScrollPane = new javax.swing.JScrollPane();
        blackCapturedPiecesLIST = new javax.swing.JList<>();
        whiteCapturedPiecesLIST = new javax.swing.JList<>();
        turnLBL = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        playerNameLBL = new javax.swing.JLabel();
        opponentNameLBL = new javax.swing.JLabel();

        // Left player name
        leftNamePanel = new JPanel(new GridBagLayout());
        leftNamePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        playerNameLBL.setText("Player 2");
        playerNameLBL.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        leftNamePanel.add(playerNameLBL);
        leftNamePanel.setPreferredSize(new Dimension(150, 30));

        // Right player name
        rightNamePanel = new JPanel(new GridBagLayout());
        rightNamePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        opponentNameLBL.setText("OPPONENT");
        opponentNameLBL.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        rightNamePanel.add(opponentNameLBL);
        rightNamePanel.setPreferredSize(new Dimension(150, 30));

        // Score labels
        blackScoreLabel = new javax.swing.JLabel();
        whiteScoreLabel = new javax.swing.JLabel();
        blackScoreLabel.setText("Score: +0");
        whiteScoreLabel.setText("Score: +0");
        blackScoreLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        whiteScoreLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        blackScoreLabel.setOpaque(true);
        whiteScoreLabel.setOpaque(true);

        // Black captured pieces panel
        jLabel2.setText("Black Captured");
        jLabel2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        jLabel2.setOpaque(true);
        jScrollPane2.setViewportView(blackCapturedPiecesLIST);
        jScrollPane2.setPreferredSize(new Dimension(150, 80));
        jScrollPane2.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));

        // White captured pieces panel
        whiteCapturedLBL = new JLabel("White Captured");
        whiteCapturedLBL.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        whiteCapturedLBL.setOpaque(true);
        rightScrollPane.setViewportView(whiteCapturedPiecesLIST);
        rightScrollPane.setPreferredSize(new Dimension(150, 80));
        rightScrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));

        JPanel chatInputPanel = new JPanel();
        quitGameBTN = new javax.swing.JButton();
        saveGameBTN = new javax.swing.JButton();

        // Conditionally initialize chat components only if not in AI mode
        if (!isAIMode) {
            chatScrollPane = new javax.swing.JScrollPane();
            chatLIST = new javax.swing.JList<>();
            chatInputTXT = new javax.swing.JTextField();
            sendBTN = new javax.swing.JButton();
            chatLBL = new javax.swing.JLabel();
            
            // Chat setup
            chatLBL.setText("CHAT BOX");
            chatLBL.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
            chatLBL.setForeground(Color.BLACK);
            chatScrollPane.setViewportView(chatLIST);
            chatScrollPane.setBorder(BorderFactory.createLineBorder(new Color(40, 167, 69), 2));
            chatScrollPane.setPreferredSize(new Dimension(300, 120));
            chatLIST.setBackground(new java.awt.Color(15, 15, 15));
            chatLIST.setForeground(Color.WHITE);
            chatLIST.setSelectionBackground(new Color(60, 60, 60));
            sendBTN.setText("Send");
            chatInputTXT.setToolTipText("Type your message here...");
            chatInputTXT.setPreferredSize(new Dimension(180, 30));
            sendBTN.setPreferredSize(new Dimension(64, 30));
        }

        // Buttons
        quitGameBTN.setText("Quit Game");
        quitGameBTN.setBackground(Color.BLACK);
        quitGameBTN.setForeground(Color.RED);
        quitGameBTN.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        quitGameBTN.setOpaque(true);

        saveGameBTN.setText("Save Game");
        saveGameBTN.setBackground(Color.BLACK);
        saveGameBTN.setForeground(Color.DARK_GRAY);
        saveGameBTN.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        saveGameBTN.setOpaque(true);
        
        // Tutor button - same for both AI and multiplayer mode
        hintBTN = new javax.swing.JButton();
        hintBTN.setText("TUTOR");
        hintBTN.setBackground(new Color(40, 167, 69)); // Green
        hintBTN.setForeground(Color.WHITE);
        hintBTN.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        hintBTN.setOpaque(true);
        hintBTN.setPreferredSize(new Dimension(100, 35));
        
        // Tutor counter label (hidden for unlimited hints)
        hintCounterLBL = new javax.swing.JLabel();
        hintCounterLBL.setText("");
        hintCounterLBL.setVisible(false); // Hide the label since hints are unlimited
        hintCounterLBL.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 10));
        hintCounterLBL.setForeground(Color.BLACK);
        hintCounterLBL.setHorizontalAlignment(SwingConstants.CENTER);

        // Timer components (multiplayer mode only)
        if (!isAIMode) {
            initTimerComponents();
        }
        
        // Chat input layout (only for multiplayer mode)
        if (!isAIMode) {
            javax.swing.GroupLayout chatInputLayout = new javax.swing.GroupLayout(chatInputPanel);
            chatInputPanel.setLayout(chatInputLayout);
            chatInputLayout.setHorizontalGroup(
                    chatInputLayout.createSequentialGroup()
                            .addComponent(chatInputTXT)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(sendBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
            );
            chatInputLayout.setVerticalGroup(
                    chatInputLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(chatInputTXT, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(sendBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
            );
        }

        // Layouts
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        
        if (isAIMode) {
            // AI mode layout without chat
            layout.setHorizontalGroup(
                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                    .addGap(50)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(leftNamePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel2)
                                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(blackScoreLabel))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                            .addComponent(hintCounterLBL)
                                            .addComponent(hintBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(layout.createSequentialGroup()
                                                    .addComponent(saveGameBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(50)
                                                    .addComponent(quitGameBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(rightNamePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(whiteCapturedLBL)
                                            .addComponent(rightScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(whiteScoreLabel))
                                    .addGap(50))
            );
        } else {
            // Multiplayer mode layout with chat
            layout.setHorizontalGroup(
                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                    .addGap(23)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(leftNamePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel2)
                                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(blackScoreLabel)
                                                .addGap(10)
                                                .addComponent(blackTimerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                            .addComponent(chatLBL)
                                            .addComponent(chatScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(chatInputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 250, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(hintCounterLBL)
                                            .addComponent(hintBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(layout.createSequentialGroup()
                                                    .addComponent(saveGameBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(100)
                                                    .addComponent(quitGameBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(rightNamePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(whiteCapturedLBL)
                                            .addComponent(rightScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(layout.createSequentialGroup()
                                                .addComponent(whiteScoreLabel)
                                                .addGap(10)
                                                .addComponent(whiteTimerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                    .addGap(30))
            );
        }

        if (isAIMode) {
            // AI mode vertical layout without chat
            layout.setVerticalGroup(
                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(layout.createSequentialGroup()
                                                    .addComponent(leftNamePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(18)
                                                    .addComponent(jLabel2)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(blackScoreLabel))
                                            .addGroup(layout.createSequentialGroup()
                                                    .addGap(30)
                                                    .addComponent(hintCounterLBL)
                                                    .addGap(5)
                                                    .addComponent(hintBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(10)
                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                            .addComponent(saveGameBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                            .addComponent(quitGameBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                            .addGroup(layout.createSequentialGroup()
                                                    .addComponent(rightNamePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(18)
                                                    .addComponent(whiteCapturedLBL)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                    .addComponent(rightScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(whiteScoreLabel)))
                                    .addGap(17))
            );
        } else {
            // Multiplayer mode vertical layout with chat
            layout.setVerticalGroup(
                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                    .addContainerGap()
                                    .addComponent(chatLBL)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addGroup(layout.createSequentialGroup()
                                                    .addComponent(leftNamePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(18)
                                                    .addComponent(jLabel2)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(blackScoreLabel)
                                                        .addComponent(blackTimerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                            .addGroup(layout.createSequentialGroup()
                                                    .addComponent(chatScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(chatInputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(5)
                                                    .addComponent(hintCounterLBL)
                                                    .addGap(3)
                                                    .addComponent(hintBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(5)
                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                            .addComponent(saveGameBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                            .addComponent(quitGameBTN, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                            .addGroup(layout.createSequentialGroup()
                                                    .addComponent(rightNamePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addGap(18)
                                                    .addComponent(whiteCapturedLBL)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                    .addComponent(rightScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                                        .addComponent(whiteScoreLabel)
                                                        .addComponent(whiteTimerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE))))
                                    .addGap(17))
            );
        }
    }

    /**
     * Initialize timer components for multiplayer mode
     */
    private void initTimerComponents() {
        whiteTimerLabel = new javax.swing.JLabel();
        blackTimerLabel = new javax.swing.JLabel();
        
        // Timer labels - smaller and positioned next to scores
        whiteTimerLabel.setText("10:00");
        whiteTimerLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        whiteTimerLabel.setOpaque(true);
        whiteTimerLabel.setBackground(Color.BLACK);
        whiteTimerLabel.setForeground(Color.WHITE);
        whiteTimerLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        whiteTimerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        whiteTimerLabel.setPreferredSize(new Dimension(80, 20));
        
        blackTimerLabel.setText("10:00");
        blackTimerLabel.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        blackTimerLabel.setOpaque(true);
        blackTimerLabel.setBackground(Color.WHITE);
        blackTimerLabel.setForeground(Color.BLACK);
        blackTimerLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        blackTimerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        blackTimerLabel.setPreferredSize(new Dimension(80, 20));
        
        // Initialize timers with default 10 minutes
        whiteTimer = new javax.swing.Timer(1000, e -> updateWhiteTimer());
        blackTimer = new javax.swing.Timer(1000, e -> updateBlackTimer());
        
        // Set initial time (10 minutes = 600 seconds)
        whiteTimeRemaining = 600;
        blackTimeRemaining = 600;
        updateTimerDisplay();
    }
    
    /**
     * Reset timers to initial state for new game
     */
    public void resetTimers() {
        // Stop any running timers
        if (whiteTimer != null) {
            whiteTimer.stop();
        }
        if (blackTimer != null) {
            blackTimer.stop();
        }
        
        // Reset timer states
        whiteTimerActive = false;
        blackTimerActive = false;
        gameTimersPaused = false;
        timersInitialized = false;
        isWhiteTurn = true; // Reset to white's turn
        
        // Reset time to initial values (10 minutes = 600 seconds)
        whiteTimeRemaining = 600;
        blackTimeRemaining = 600;
        
        // Update display
        if (whiteTimerLabel != null && blackTimerLabel != null) {
            updateTimerDisplay();
            updateTimerHighlight();
        }
        
        System.out.println("DEBUG: Timers reset for new game");
    }
    
    /**
     * Start the timers
     */
    public void startTimers() {
        // Skip if this is AI mode
        if (isAIMode) {
            System.out.println("DEBUG: Skipping timer start - AI mode");
            return;
        }
        
        // Stop any running timers first
        if (whiteTimer != null) {
            whiteTimer.stop();
        }
        if (blackTimer != null) {
            blackTimer.stop();
        }
        
        if (!timersInitialized) {
            // Set initial time (10 minutes = 600 seconds)
            whiteTimeRemaining = 600;
            blackTimeRemaining = 600;
            updateTimerDisplay();
            timersInitialized = true;
        }
        
        gameTimersPaused = false;
        
        // Start the timer for the current turn
        if (isWhiteTurn) {
            whiteTimer.start();
            whiteTimerActive = true;
            blackTimerActive = false;
            updateTimerHighlight();
            System.out.println("DEBUG: Started WHITE timer");
        } else {
            blackTimer.start();
            blackTimerActive = true;
            whiteTimerActive = false;
            updateTimerHighlight();
            System.out.println("DEBUG: Started BLACK timer");
        }
    }
    
    /**
     * Pause the timers
     */
    public void pauseTimers() {
        gameTimersPaused = true;
        whiteTimer.stop();
        blackTimer.stop();
        whiteTimerActive = false;
        blackTimerActive = false;
        
        updateTimerHighlight();
    }
    
    /**
     * Switch timer turn (pause current, start other)
     */
    public void switchTimerTurn() {
        if (!timersInitialized || gameTimersPaused) {
            return; // Don't switch if timers aren't running
        }
        
        isWhiteTurn = !isWhiteTurn;
        
        // Stop both timers immediately and synchronously
        synchronized (this) {
            whiteTimer.stop();
            blackTimer.stop();
            
            // Small delay to ensure complete stop before restart
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Start the appropriate timer
            if (isWhiteTurn) {
                whiteTimer.start();
                whiteTimerActive = true;
                blackTimerActive = false;
            } else {
                blackTimer.start();
                blackTimerActive = true;
                whiteTimerActive = false;
            }
        }
        
        updateTimerHighlight();
        System.out.println("DEBUG: Timer switched to " + (isWhiteTurn ? "WHITE" : "BLACK") + " with improved synchronization");
    }
    
    
    /**
     * Update white timer (countdown)
     */
    private void updateWhiteTimer() {
        if (whiteTimeRemaining > 0) {
            whiteTimeRemaining--;
            updateTimerDisplay();
        } else {
            // White time expired
            whiteTimer.stop();
            whiteTimerActive = false;
            JOptionPane.showMessageDialog(this, "White's time has expired! Black wins!", "Time's Up!", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Update black timer (countdown)
     */
    private void updateBlackTimer() {
        if (blackTimeRemaining > 0) {
            blackTimeRemaining--;
            updateTimerDisplay();
        } else {
            // Black time expired
            blackTimer.stop();
            blackTimerActive = false;
            JOptionPane.showMessageDialog(this, "Black's time has expired! White wins!", "Time's Up!", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Update timer display
     */
    private void updateTimerDisplay() {
        int whiteMinutes = whiteTimeRemaining / 60;
        int whiteSeconds = whiteTimeRemaining % 60;
        int blackMinutes = blackTimeRemaining / 60;
        int blackSeconds = blackTimeRemaining % 60;
        
        whiteTimerLabel.setText(String.format("White: %d:%02d", whiteMinutes, whiteSeconds));
        blackTimerLabel.setText(String.format("Black: %d:%02d", blackMinutes, blackSeconds));
        
        // Color coding for low time
        if (whiteTimeRemaining <= 30) {
            whiteTimerLabel.setForeground(Color.RED);
        } else if (whiteTimeRemaining <= 60) {
            whiteTimerLabel.setForeground(Color.ORANGE);
        } else {
            whiteTimerLabel.setForeground(Color.WHITE);
        }
        
        if (blackTimeRemaining <= 30) {
            blackTimerLabel.setForeground(Color.RED);
        } else if (blackTimeRemaining <= 60) {
            blackTimerLabel.setForeground(Color.ORANGE);
        } else {
            blackTimerLabel.setForeground(Color.BLACK);
        }
    }
    
    /**
     * Update timer highlight to show whose turn it is
     */
    private void updateTimerHighlight() {
        if (whiteTimerActive) {
            whiteTimerLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
            blackTimerLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        } else if (blackTimerActive) {
            blackTimerLabel.setBorder(BorderFactory.createLineBorder(Color.GREEN, 3));
            whiteTimerLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        } else {
            whiteTimerLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
            blackTimerLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        }
    }
    
    /**
     * Switch timers when turn changes
     */
    public void switchTurn() {
        if (gameTimersPaused) return;
        
        isWhiteTurn = !isWhiteTurn;
        
        if (whiteTimerActive || blackTimerActive) {
            whiteTimer.stop();
            blackTimer.stop();
            
            if (isWhiteTurn) {
                whiteTimer.start();
                whiteTimerActive = true;
                blackTimerActive = false;
            } else {
                blackTimer.start();
                blackTimerActive = true;
                whiteTimerActive = false;
            }
            
            updateTimerHighlight();
        }
    }
    
    
    /**
     * Check if timers are running
     */
    public boolean areTimersRunning() {
        return whiteTimerActive || blackTimerActive;
    }
    
    /**
     * Get current white timer remaining time
     */
    public int getWhiteTimeRemaining() {
        return whiteTimeRemaining;
    }
    
    /**
     * Get current black timer remaining time
     */
    public int getBlackTimeRemaining() {
        return blackTimeRemaining;
    }
    
    /**
     * Get current turn (true for white, false for black)
     */
    public boolean isWhiteTurn() {
        return isWhiteTurn;
    }
    
    /**
     * Set timer state from loaded game
     */
    public void setTimerState(int whiteTime, int blackTime, boolean timersWereActive, boolean wasWhiteTurn) {
        if (isAIMode) {
            return; // Don't set timer state for AI mode
        }
        
        this.whiteTimeRemaining = whiteTime;
        this.blackTimeRemaining = blackTime;
        this.isWhiteTurn = wasWhiteTurn;
        
        // Update timer display
        updateTimerDisplay();
        
        // Reset timer states
        whiteTimerActive = false;
        blackTimerActive = false;
        gameTimersPaused = !timersWereActive;
        
        updateTimerHighlight();
        
        System.out.println("DEBUG: Timer state loaded - White: " + whiteTime + "s, Black: " + blackTime + "s, Active: " + timersWereActive + ", Turn: " + (wasWhiteTurn ? "WHITE" : "BLACK"));
    }
    
    /**
     * Auto-restart timers after loading with proper synchronization
     */
    public void restartTimersAfterLoad(boolean timersWereActive) {
        if (isAIMode || !timersWereActive) {
            return;
        }
        
        // Small delay to ensure both clients are ready
        javax.swing.Timer restartTimer = new javax.swing.Timer(500, e -> {
            if (isWhiteTurn) {
                whiteTimer.start();
                whiteTimerActive = true;
                blackTimerActive = false;
            } else {
                blackTimer.start();
                blackTimerActive = true;
                whiteTimerActive = false;
            }
            
            gameTimersPaused = false;
            timersInitialized = true;
            updateTimerHighlight();
            
            System.out.println("DEBUG: Timers restarted after load for " + (isWhiteTurn ? "WHITE" : "BLACK"));
            ((javax.swing.Timer) e.getSource()).stop();
        });
        restartTimer.setRepeats(false);
        restartTimer.start();
    }
    
    
    /**
     * Add a captured piece to the appropriate list and update scores
     * @param pieceString String representation of the captured piece (e.g., "WHITE QUEEN")
     * @param pieceValue Point value of the captured piece
     */
    public void addCapturedPiece(String pieceString, int pieceValue) {
        System.out.println("DEBUG: Adding captured piece: '" + pieceString + "' with value: " + pieceValue);
        System.out.println("DEBUG: Current blackScore: " + blackScore + ", whiteScore: " + whiteScore);
        
        // Normalize the string to handle any case variations
        String normalizedPiece = pieceString.trim().toUpperCase();
        
        if (normalizedPiece.contains("BLACK")) {
            // Black piece was captured - add to black captured list
            String simplePieceName = normalizedPiece.replace("BLACK ", "").toLowerCase();
            simplePieceName = simplePieceName.substring(0, 1).toUpperCase() + simplePieceName.substring(1);
            blackCapturedPiecesListModel.addElement(simplePieceName);
            blackScore += pieceValue;
            
            System.out.println("DEBUG: BLACK piece captured! Added '" + simplePieceName + "' to black captured list");
            System.out.println("DEBUG: New blackScore: " + blackScore);
            
            // Auto-scroll to bottom
            SwingUtilities.invokeLater(() -> {
                int lastIndex = blackCapturedPiecesLIST.getModel().getSize() - 1;
                if (lastIndex >= 0) {
                    blackCapturedPiecesLIST.ensureIndexIsVisible(lastIndex);
                }
                blackCapturedPiecesLIST.repaint();
            });
        } else if (normalizedPiece.contains("WHITE")) {
            // White piece was captured - add to white captured list
            String simplePieceName = normalizedPiece.replace("WHITE ", "").toLowerCase();
            simplePieceName = simplePieceName.substring(0, 1).toUpperCase() + simplePieceName.substring(1);
            whiteCapturedPiecesListModel.addElement(simplePieceName);
            whiteScore += pieceValue;
            
            System.out.println("DEBUG: WHITE piece captured! Added '" + simplePieceName + "' to white captured list");
            System.out.println("DEBUG: New whiteScore: " + whiteScore);
            
            // Auto-scroll to bottom
            SwingUtilities.invokeLater(() -> {
                int lastIndex = whiteCapturedPiecesLIST.getModel().getSize() - 1;
                if (lastIndex >= 0) {
                    whiteCapturedPiecesLIST.ensureIndexIsVisible(lastIndex);
                }
                whiteCapturedPiecesLIST.repaint();
            });
        } else {
            System.out.println("DEBUG: ERROR - Piece string '" + pieceString + "' does not contain BLACK or WHITE!");
        }
        
        System.out.println("DEBUG: Calling updateScoreDisplay()...");
        updateScoreDisplay();
        
        // Force a repaint of both panels
        SwingUtilities.invokeLater(() -> {
            this.revalidate();
            this.repaint();
        });
    }
    
    /**
     * Update the score display showing relative advantage
     */
    private void updateScoreDisplay() {
        System.out.println("DEBUG: updateScoreDisplay() called - blackScore: " + blackScore + ", whiteScore: " + whiteScore);
        
        int scoreDifference = whiteScore - blackScore;
        System.out.println("DEBUG: Score difference (white - black): " + scoreDifference);
        
        if (scoreDifference > 0) {
            // White is ahead
            whiteScoreLabel.setText("Score: +" + scoreDifference);
            blackScoreLabel.setText("Score: -" + scoreDifference);
            whiteScoreLabel.setForeground(new Color(0, 150, 0)); // Green for advantage
            blackScoreLabel.setForeground(Color.RED); // Red for disadvantage
            System.out.println("DEBUG: White is ahead by " + scoreDifference);
        } else if (scoreDifference < 0) {
            // Black is ahead
            int blackAdvantage = Math.abs(scoreDifference);
            blackScoreLabel.setText("Score: +" + blackAdvantage);
            whiteScoreLabel.setText("Score: -" + blackAdvantage);
            blackScoreLabel.setForeground(new Color(0, 150, 0)); // Green for advantage
            whiteScoreLabel.setForeground(Color.RED); // Red for disadvantage
            System.out.println("DEBUG: Black is ahead by " + blackAdvantage);
        } else {
            // Equal
            whiteScoreLabel.setText("Score: +0");
            blackScoreLabel.setText("Score: +0");
            whiteScoreLabel.setForeground(Color.BLACK);
            blackScoreLabel.setForeground(Color.BLACK);
            System.out.println("DEBUG: Scores are equal");
        }
        
        // Update score label backgrounds to match their respective panels
        whiteScoreLabel.setBackground(playerTeam == Team.WHITE ? Color.BLACK : Color.WHITE);
        blackScoreLabel.setBackground(playerTeam == Team.WHITE ? Color.WHITE : Color.BLACK);
        
        System.out.println("DEBUG: Score labels updated - White: '" + whiteScoreLabel.getText() + "', Black: '" + blackScoreLabel.getText() + "'");
        
        // Force immediate repaint
        SwingUtilities.invokeLater(() -> {
            whiteScoreLabel.repaint();
            blackScoreLabel.repaint();
        });
    }
    
    /**
     * Reset scores and captured pieces lists
     */
    public void resetGame() {
        whiteScore = 0;
        blackScore = 0;
        blackCapturedPiecesListModel.clear();
        whiteCapturedPiecesListModel.clear();
        updateScoreDisplay();
    }
    
    /**
     * Get current score difference (positive means white ahead, negative means black ahead)
     */
    public int getScoreDifference() {
        return whiteScore - blackScore;
    }
    
    /**
     * Get white's captured material points
     */
    public int getWhiteScore() {
        return whiteScore;
    }
    
    /**
     * Get black's captured material points
     */
    public int getBlackScore() {
        return blackScore;
    }
}
