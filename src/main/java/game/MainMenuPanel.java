package game;

import javax.swing.*;
import java.awt.*;

public class MainMenuPanel extends JPanel {
    private GameStartListener listener;

    public interface GameStartListener {
        void onStartGame();
        void onExitGame();
    }

    public MainMenuPanel(GameStartListener listener) {
        this.listener = listener;
        setLayout(new GridBagLayout());
        setBackground(Color.BLACK);

        JLabel title = new JLabel("SKELETON KILLER");
        title.setFont(new Font("Serif", Font.BOLD, 48));
        title.setForeground(new Color(255, 230, 100));

        JButton startButton = new JButton("START GAME");
        JButton exitButton = new JButton("EXIT");

        startButton.setFont(new Font("Arial", Font.BOLD, 24));
        exitButton.setFont(new Font("Arial", Font.BOLD, 24));

        startButton.setFocusPainted(false);
        exitButton.setFocusPainted(false);

        startButton.addActionListener(e -> listener.onStartGame());
        exitButton.addActionListener(e -> listener.onExitGame());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 0, 20, 0);
        gbc.gridx = 0;

        gbc.gridy = 0;
        add(title, gbc);

        gbc.gridy = 1;
        add(startButton, gbc);

        gbc.gridy = 2;
        add(exitButton, gbc);
    }
}
