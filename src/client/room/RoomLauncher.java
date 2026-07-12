package client.room;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import client.GameClient;
import client.draw.DrawController;
import client.draw.DrawPanel;
import client.game.ChatPanel;
import client.game.GameController;
import client.game.GamePanel;

public class RoomLauncher {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? parsePort(args[1]) : 8080;

        GameClient client = new GameClient();
        String initialStatus = "サーバーに接続しました: " + host + ":" + port;
        try {
            client.connect(host, port);
        } catch (Exception e) {
            client = null;
            initialStatus = "サーバーに接続できませんでした: " + e.getMessage();
        }

        GameClient connectedClient = client;
        String status = initialStatus;
        SwingUtilities.invokeLater(() -> buildAndShow(connectedClient, status));
    }

    private static void buildAndShow(GameClient client, String initialStatus) {
        RoomPanel roomPanel = new RoomPanel();
        DrawPanel drawPanel = new DrawPanel();
        ChatPanel chatPanel = new ChatPanel();
        GamePanel gamePanel = new GamePanel(chatPanel);

        RoomController.init(client, roomPanel);
        DrawController.init(client, drawPanel);
        GameController.init(client, chatPanel, gamePanel);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Room", roomPanel);
        tabs.addTab("Draw", buildDrawPanel(drawPanel));
        tabs.addTab("Game", gamePanel);

        JFrame frame = new JFrame("お絵描きの森");
        frame.setLayout(new BorderLayout());
        frame.add(tabs, BorderLayout.CENTER);
        frame.setSize(980, 720);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        roomPanel.showStatus(initialStatus);
        if (client != null) {
            RoomController.requestRoomList();
        }
    }

    private static JPanel buildDrawPanel(DrawPanel drawPanel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buildDrawToolBar(drawPanel), BorderLayout.NORTH);
        panel.add(drawPanel, BorderLayout.CENTER);
        return panel;
    }

    private static JToolBar buildDrawToolBar(DrawPanel drawPanel) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        addColorButton(toolBar, drawPanel, "Black", "BLACK");
        addColorButton(toolBar, drawPanel, "Red", "RED");
        addColorButton(toolBar, drawPanel, "Blue", "BLUE");
        addColorButton(toolBar, drawPanel, "Green", "GREEN");
        addColorButton(toolBar, drawPanel, "Yellow", "YELLOW");
        addColorButton(toolBar, drawPanel, "Eraser", "WHITE");

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> drawPanel.clearCanvas());
        toolBar.add(clearButton);

        return toolBar;
    }

    private static void addColorButton(JToolBar toolBar, DrawPanel drawPanel, String label, String colorName) {
        JButton button = new JButton(label);
        button.addActionListener(e -> drawPanel.setColorName(colorName));
        toolBar.add(button);
    }

    private static int parsePort(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            System.out.println("ポート番号が不正なので8080を使います: " + s);
            return 8080;
        }
    }
}
