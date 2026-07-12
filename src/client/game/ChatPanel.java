package client.game;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class ChatPanel extends JPanel {
    private final DefaultListModel<String> chatModel = new DefaultListModel<>();
    private final JList<String> chatList = new JList<>(chatModel);
    private final JTextField inputField = new JTextField(24);
    private final JButton sendButton = new JButton("Send");
    private final JLabel roleLabel = new JLabel("Role: -");
    private final JLabel themeLabel = new JLabel("Theme: -");
    private final JLabel timerLabel = new JLabel("Time: -");
    private final JLabel resultLabel = new JLabel(" ");

    public ChatPanel() {
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createTitledBorder("Chat"));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        statusPanel.add(roleLabel);
        statusPanel.add(themeLabel);
        statusPanel.add(timerLabel);
        add(statusPanel, BorderLayout.NORTH);

        add(new JScrollPane(chatList), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(6, 0));
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(resultLabel, BorderLayout.SOUTH);
        add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(this::send);
        inputField.addActionListener(this::send);
    }

    public void addChatMessage(String message) {
        chatModel.addElement(message);
        chatList.ensureIndexIsVisible(chatModel.getSize() - 1);
    }

    public void clearChat() {
        chatModel.clear();
        resultLabel.setText(" ");
    }

    public void setRoundInfo(String role, String drawerName, String theme) {
        roleLabel.setText("Role: " + role + " / Drawer: " + drawerName);
        themeLabel.setText(theme == null || theme.isEmpty() ? "Theme: -" : "Theme: " + theme);
    }

    public void setTimeRemaining(int seconds) {
        timerLabel.setText("Time: " + seconds);
    }

    public void showResult(String message) {
        resultLabel.setText(message == null || message.isEmpty() ? " " : message);
    }

    private void send(ActionEvent event) {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        inputField.setText("");
        GameController.submitChat(text);
    }
}
