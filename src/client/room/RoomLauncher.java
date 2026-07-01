package client.room;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import client.GameClient;

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
        RoomPanel panel = new RoomPanel();
        RoomController.init(client, panel);

        JFrame frame = new JFrame("お絵描きの森 - ロビー");
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.setSize(640, 420);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        panel.showStatus(initialStatus);
        if (client != null) {
            RoomController.requestRoomList();
        }
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
