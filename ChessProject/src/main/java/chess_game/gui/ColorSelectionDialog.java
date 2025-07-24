package chess_game.gui;

import chess_game.Pieces.Team;
import javax.swing.*;
import java.awt.*;

/**
 * Dialog for selecting player color in AI mode
 */
public class ColorSelectionDialog extends JDialog {
    
    private Team selectedTeam;
    private boolean colorSelected = false;
    
    public ColorSelectionDialog(JFrame parent) {
        super(parent, "Choose Your Color", true);
        initComponents();
    }
    
    private void initComponents() {
        setSize(400, 300);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Create main panel with background
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(240, 240, 240));
        
        // Title label
        JLabel titleLabel = new JLabel("Choose Your Color");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        
        // Instructions label
        JLabel instructionLabel = new JLabel("Select the color you want to play as:");
        instructionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        instructionLabel.setForeground(Color.DARK_GRAY);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        buttonPanel.setOpaque(false);
        
        // White button
        JButton whiteButton = createColorButton("WHITE", "You move first", Color.WHITE, Color.BLACK);
        whiteButton.addActionListener(e -> {
            selectedTeam = Team.WHITE;
            colorSelected = true;
            dispose();
        });
        
        // Black button
        JButton blackButton = createColorButton("BLACK", "AI moves first", Color.BLACK, Color.WHITE);
        blackButton.addActionListener(e -> {
            selectedTeam = Team.BLACK;
            colorSelected = true;
            dispose();
        });
        
        buttonPanel.add(whiteButton);
        buttonPanel.add(blackButton);
        
        // Info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);
        infoPanel.add(instructionLabel, BorderLayout.CENTER);
        
        JLabel tipLabel = new JLabel("<html><center>White pieces move first in chess.<br>Choose White for an easier start!</center></html>");
        tipLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        tipLabel.setHorizontalAlignment(SwingConstants.CENTER);
        tipLabel.setForeground(Color.GRAY);
        tipLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        infoPanel.add(tipLabel, BorderLayout.SOUTH);
        
        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.addActionListener(e -> dispose());
        
        JPanel cancelPanel = new JPanel(new FlowLayout());
        cancelPanel.setOpaque(false);
        cancelPanel.add(cancelButton);
        
        // Add components to main panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(titleLabel, BorderLayout.CENTER);
        topPanel.add(infoPanel, BorderLayout.SOUTH);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(cancelPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JButton createColorButton(String colorName, String description, Color bgColor, Color textColor) {
        JButton button = new JButton();
        button.setLayout(new BorderLayout());
        button.setPreferredSize(new Dimension(140, 100));
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createRaisedBevelBorder(),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        button.setFocusPainted(false);
        // Color name label
        JLabel nameLabel = new JLabel(colorName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        nameLabel.setForeground(textColor);
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Description label
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(textColor);
        descLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        // Chess piece symbol
        JLabel symbolLabel = new JLabel(colorName.equals("WHITE") ? "♔" : "♚");
        symbolLabel.setFont(new Font("Serif", Font.PLAIN, 24));
        symbolLabel.setForeground(textColor);
        symbolLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        button.add(symbolLabel, BorderLayout.NORTH);
        button.add(nameLabel, BorderLayout.CENTER);
        button.add(descLabel, BorderLayout.SOUTH);
        
        // Hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                    BorderFactory.createEmptyBorder(9, 9, 9, 9)
                ));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createRaisedBevelBorder(),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
            }
        });

        return button;
    }
    /**
     * Check if a color was selected
     * @return true if a color was selected, false if dialog was cancelled
     */
    public boolean isColorSelected() {
        return colorSelected;
    }
    
    /**
     * Get the selected team
     * @return The selected team (WHITE or BLACK)
     */
    public Team getSelectedTeam() {
        return selectedTeam;
    }
}
