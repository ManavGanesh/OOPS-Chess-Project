package chess_game.gui;

import Messages.PlayerInfo;
import chess_game.Pieces.Team;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 *
 * @author Enes Kızılcın <nazifenes.kizilcin@stu.fsm.edu.tr>
 */
public class PlayerSelectionPanel extends javax.swing.JPanel {

    private javax.swing.JLabel titleLBL;
    private javax.swing.JScrollPane playersScrollPane;
    private javax.swing.JList<String> playersLIST;
    private javax.swing.JButton sendRequestBTN;
    private javax.swing.JButton refreshBTN;
    private javax.swing.JButton backBTN;
    private javax.swing.JLabel statusLBL;
    private javax.swing.JComboBox<String> timeSelectionCombo;
    private javax.swing.JLabel timeSelectionLBL;
    public javax.swing.JComboBox<String> colorSelectionCombo;
    private javax.swing.JLabel colorSelectionLBL;
    private java.awt.image.BufferedImage backgroundImage;
    
    public DefaultListModel<String> playersListModel;
    private ArrayList<PlayerInfo> availablePlayers;
    
    public PlayerSelectionPanel() {
        // Load the same background image as MainMenu
        try {
            backgroundImage = javax.imageio.ImageIO.read(getClass().getClassLoader()
                    .getResourceAsStream("chess_game/Img/Main_menu_bgd_logo.png"));
        } catch (java.io.IOException | IllegalArgumentException e) {
            System.err.println("Failed to load background image: " + e.getMessage());
        }
        
        initComponents();
        playersListModel = new DefaultListModel<>();
        playersLIST.setModel(playersListModel);
        availablePlayers = new ArrayList<>();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw background image scaled to panel size
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    public JList<String> getPlayersLIST() {
        return playersLIST;
    }

    public JButton getSendRequestBTN() {
        return sendRequestBTN;
    }

    public JButton getRefreshBTN() {
        return refreshBTN;
    }

    public JButton getBackBTN() {
        return backBTN;
    }

    public JLabel getStatusLBL() {
        return statusLBL;
    }

    public void updatePlayersList(ArrayList<PlayerInfo> players) {
        System.out.println("DEBUG: PlayerSelectionPanel.updatePlayersList called with " + (players != null ? players.size() : "null") + " players");
        
        this.availablePlayers = players;
        playersListModel.clear();
        
        if (players != null) {
            for (PlayerInfo player : players) {
                playersListModel.addElement(player.playerName);
                System.out.println("DEBUG: Added player to list model: " + player.playerName);
            }
        }
        
        // Force UI update
        playersLIST.revalidate();
        playersLIST.repaint();
        
        System.out.println("DEBUG: PlayerSelectionPanel list model now has " + playersListModel.getSize() + " items");
    }

    public PlayerInfo getSelectedPlayer() {
        String selectedName = playersLIST.getSelectedValue();
        if (selectedName != null) {
            for (PlayerInfo player : availablePlayers) {
                if (player.playerName.equals(selectedName)) {
                    return player;
                }
            }
        }
        return null;
    }
    
    public String getSelectedGameTime() {
        return (String) timeSelectionCombo.getSelectedItem();
    }
    
    public Team getSelectedColor() {
        String selectedColor = (String) colorSelectionCombo.getSelectedItem();
        if ("White".equals(selectedColor)) {
            return Team.WHITE;
        } else if ("Black".equals(selectedColor)) {
            return Team.BLACK;
        } else {
            return Team.WHITE; // Default to white if random is selected
        }
    }

    private void initComponents() {
        titleLBL = new javax.swing.JLabel();
        playersScrollPane = new javax.swing.JScrollPane();
        playersLIST = new javax.swing.JList<>();
        sendRequestBTN = new javax.swing.JButton();
        refreshBTN = new javax.swing.JButton();
        backBTN = new javax.swing.JButton();
        statusLBL = new javax.swing.JLabel();
        timeSelectionCombo = new javax.swing.JComboBox<>();
        timeSelectionLBL = new javax.swing.JLabel();
        colorSelectionCombo = new javax.swing.JComboBox<>();
        colorSelectionLBL = new javax.swing.JLabel();

        // Title styling
        titleLBL.setFont(new java.awt.Font("Segoe UI", 1, 24));
        titleLBL.setText("Select Player to Challenge");
        titleLBL.setHorizontalAlignment(SwingConstants.CENTER);
        titleLBL.setForeground(Color.WHITE);

        // Players list styling
        playersScrollPane.setViewportView(playersLIST);
        playersLIST.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playersLIST.setFont(new java.awt.Font("Segoe UI", 0, 14));
        playersLIST.setBackground(new Color(240, 240, 240));
        playersLIST.setSelectionBackground(new Color(70, 130, 180));
        playersLIST.setSelectionForeground(Color.WHITE);
        playersScrollPane.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));

        // Button styling
        sendRequestBTN.setText("Send Play Request");
        refreshBTN.setText("Refresh List");
        backBTN.setText("Back to Menu");
        
        // Style all buttons consistently
        javax.swing.JButton[] buttons = {sendRequestBTN, refreshBTN, backBTN};
        for (javax.swing.JButton btn : buttons) {
            btn.setFont(new java.awt.Font("Segoe UI", 1, 12));
            btn.setPreferredSize(new Dimension(130, 35));
            btn.setBackground(new Color(70, 130, 180));
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);
            btn.setBorder(BorderFactory.createRaisedBevelBorder());
        }
        
        // Status label styling
        statusLBL.setText("Select a player and click 'Send Play Request'");
        statusLBL.setForeground(Color.WHITE);
        statusLBL.setFont(new java.awt.Font("Segoe UI", 0, 14));
        statusLBL.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Time selection styling
        timeSelectionLBL.setText("Game Time:");
        timeSelectionLBL.setForeground(Color.WHITE);
        timeSelectionLBL.setFont(new java.awt.Font("Segoe UI", 1, 12));
        
        timeSelectionCombo.addItem("1 minute");
        timeSelectionCombo.addItem("3 minutes");
        timeSelectionCombo.addItem("5 minutes");
        timeSelectionCombo.addItem("10 minutes");
        timeSelectionCombo.addItem("15 minutes");
        timeSelectionCombo.addItem("30 minutes");
        timeSelectionCombo.setSelectedItem("10 minutes");
        timeSelectionCombo.setFont(new java.awt.Font("Segoe UI", 0, 12));
        timeSelectionCombo.setPreferredSize(new Dimension(120, 25));
        
        // Color selection styling
        colorSelectionLBL.setText("Your Color:");
        colorSelectionLBL.setForeground(Color.WHITE);
        colorSelectionLBL.setFont(new java.awt.Font("Segoe UI", 1, 12));
        
        colorSelectionCombo.addItem("White");
        colorSelectionCombo.addItem("Black");
        colorSelectionCombo.addItem("Random");
        colorSelectionCombo.setSelectedItem("White");
        colorSelectionCombo.setFont(new java.awt.Font("Segoe UI", 0, 12));
        colorSelectionCombo.setPreferredSize(new Dimension(120, 25));

        // Create a main container panel with fixed maximum width
        JPanel mainContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        mainContainer.setOpaque(false);
        
        // Create a content panel with fixed width to prevent overstretching
        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setPreferredSize(new Dimension(400, 650)); // Increased height for new row
        contentPanel.setMaximumSize(new Dimension(400, 850));
        contentPanel.setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Add components to content panel
        gbc.gridy = 0; 
        gbc.gridwidth = 3;
        contentPanel.add(titleLBL, gbc);
        
        gbc.gridy = 1;
        gbc.ipady = 150; // Make the list taller
        // Set preferred size instead of using ipadx to control width
        playersScrollPane.setPreferredSize(new Dimension(350, 200));
        playersScrollPane.setMaximumSize(new Dimension(350, 250));
        contentPanel.add(playersScrollPane, gbc);
        gbc.ipady = 0; // Reset
        
        // Button row
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        contentPanel.add(sendRequestBTN, gbc);
        gbc.gridx = 1;
        contentPanel.add(refreshBTN, gbc);
        gbc.gridx = 2;
        contentPanel.add(backBTN, gbc);
        
        // Time selection row
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(timeSelectionLBL, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(timeSelectionCombo, gbc);
        
        // Color selection row
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        contentPanel.add(colorSelectionLBL, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(colorSelectionCombo, gbc);
        
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(statusLBL, gbc);
        
        // Add content panel to main container
        mainContainer.add(contentPanel);

        // Add the main container using BorderLayout.CENTER
        setLayout(new BorderLayout());
        add(mainContainer, BorderLayout.CENTER);
    }
}
