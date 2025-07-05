package chess_game.gui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.awt.*;

public class MainMenu extends javax.swing.JPanel {

    private javax.swing.JButton exitBTN;
    private javax.swing.JLabel infoLBL;
    private javax.swing.JButton playBTN;
    private javax.swing.JButton selectPlayerBTN; // New button
    private javax.swing.JButton loadGameBTN; // Load game button
    private javax.swing.JButton aiPlayBTN;
    private javax.swing.JTextField playerNameTXT;
    private javax.swing.JLabel playerNameLBL;
    private BufferedImage backgroundImage;

    public MainMenu() {
        // Load background image
        try {
            backgroundImage = ImageIO.read(getClass().getClassLoader()
                    .getResourceAsStream("chess_game/Img/Main_menu_bgd_logo.png"));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to load background image: " + e.getMessage());
        }

        initComponents();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw background image scaled to panel size
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    public JButton getExitBTN() {
        return exitBTN;
    }

    public JLabel getInfoLBL() {
        return infoLBL;
    }

    public JButton getPlayBTN() {
        return playBTN;
    }

    public JButton getSelectPlayerBTN() {
        return selectPlayerBTN;
    }

    public JButton getLoadGameBTN() {
        return loadGameBTN;
    }

    public JButton getAIPlayBTN() {
        return aiPlayBTN;
    }

    public JTextField getPlayerNameTXT() {
        return playerNameTXT;
    }

    public String getPlayerName() {
        String name = playerNameTXT.getText().trim();
        return name.isEmpty() ? "Anonymous" : name;
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        playBTN = new JButton("Multiplayer");
        aiPlayBTN = new JButton("AI Play");
        selectPlayerBTN = new JButton("Select Player");
        exitBTN = new JButton("Exit");
        loadGameBTN = new JButton("Load Game");
        infoLBL = new JLabel("Matching");
        playerNameLBL = new JLabel("Enter your name:");
        playerNameTXT = new JTextField("Player", 15);

        infoLBL.setVisible(false);
        playerNameLBL.setForeground(Color.WHITE);

        playerNameTXT.addKeyListener(new KeyListener() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    playBTN.doClick();
                }
            }

            @Override public void keyTyped(KeyEvent e) {}
            @Override public void keyReleased(KeyEvent e) {}
        });

        // Panel to hold all the form controls
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false); // Let background show through
        centerPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0; centerPanel.add(playerNameLBL, gbc);
        gbc.gridy = 1; centerPanel.add(playerNameTXT, gbc);
        gbc.gridy = 2; centerPanel.add(playBTN, gbc);
        gbc.gridy = 3; centerPanel.add(aiPlayBTN, gbc);
        gbc.gridy = 4; centerPanel.add(loadGameBTN, gbc);
        gbc.gridy = 5; centerPanel.add(exitBTN, gbc);
        gbc.gridy = 6; centerPanel.add(infoLBL, gbc);

        // Add the center panel using BorderLayout.CENTER
        setLayout(new BorderLayout());
        add(centerPanel, BorderLayout.CENTER);
    }

    private void exitBTNActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }
}
