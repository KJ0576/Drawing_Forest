package client.game;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class GamePanel extends JPanel {
    private final JLabel roundLabel = new JLabel("Round: -");
    private final JLabel roleLabel = new JLabel("Role: -");
    private final JLabel drawerLabel = new JLabel("Drawer: -");
    private final JLabel themeLabel = new JLabel("Theme: -");
    private final JLabel timerLabel = new JLabel("Time: -");
    private final JTextArea scoreArea = new JTextArea(6, 20);
    private final ChatPanel chatPanel;

    public GamePanel(ChatPanel chatPanel) {
        this.chatPanel = chatPanel;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildStatusPanel(), BorderLayout.NORTH);
        add(chatPanel, BorderLayout.CENTER);
        add(buildScorePanel(), BorderLayout.EAST);
    }

    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    public void setRoundInfo(String roundText, String role, String drawer, String theme) {
        roundLabel.setText("Round: " + roundText);
        roleLabel.setText("Role: " + role);
        drawerLabel.setText("Drawer: " + drawer);
        themeLabel.setText(theme == null || theme.isEmpty() ? "Theme: -" : "Theme: " + theme);
    }

    public void setTimeRemaining(int seconds) {
        timerLabel.setText("Time: " + seconds);
    }

    public void setScores(String scoreText) {
        scoreArea.setText(formatScores(scoreText));
    }

    public void showFinalScores(String scoreText) {
        scoreArea.setText("Final scores\n" + formatScores(scoreText));
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 8, 0));
        panel.add(roundLabel);
        panel.add(roleLabel);
        panel.add(drawerLabel);
        panel.add(themeLabel);
        panel.add(timerLabel);
        return panel;
    }

    private JPanel buildScorePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Scores"));
        scoreArea.setEditable(false);
        panel.add(scoreArea, BorderLayout.CENTER);
        return panel;
    }

    private String formatScores(String scoreText) {
        if (scoreText == null || scoreText.trim().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        String[] entries = scoreText.split(";");
        for (String entry : entries) {
            if (entry.trim().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(System.lineSeparator());
            }
            sb.append(entry.replace("=", ": "));
        }
        return sb.toString();
    }
}
