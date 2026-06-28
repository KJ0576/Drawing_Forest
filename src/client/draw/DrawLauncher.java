package client.draw;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import client.GameClient;

/**
 * 担当B（お絵描き）の単体テスト用ランチャー。
 *
 * GameServer を起動した状態で、このランチャーを2つ以上立ち上げると、
 * 片方で描いた線がもう片方の画面にもリアルタイムで現れることを確認できる。
 *
 * 使い方（例）:
 *   1) サーバー起動:   java server.GameServer
 *   2) クライアント1:  java client.draw.DrawLauncher localhost 8080 testroom
 *   3) クライアント2:  java client.draw.DrawLauncher localhost 8080 testroom
 *      （同じ部屋ID "testroom" を指定すること）
 *
 * 引数: [サーバーIP] [ポート] [部屋ID]   ※省略時は localhost 8080 testroom
 *
 * ※ これはB機能の確認用です。最終的には担当D/AのUIから
 *    DrawController.init(...) / setRoomId(...) を呼んで組み込みます。
 */
public class DrawLauncher {

    public static void main(String[] args) {
        String host   = (args.length > 0) ? args[0] : "localhost";
        int    port   = (args.length > 1) ? parsePort(args[1]) : 8080;
        String roomId = (args.length > 2) ? args[2] : "testroom";

        GameClient client = new GameClient();
        try {
            client.connect(host, port);
            System.out.println("サーバーに接続しました: " + host + ":" + port + " (room=" + roomId + ")");
        } catch (Exception e) {
            // 接続できなくても、描画自体のローカル確認はできるようにUIは表示する
            System.out.println("サーバーに接続できませんでした（ローカル描画のみ）: " + e.getMessage());
            client = null;
        }

        final GameClient connected = client;
        final String room = roomId;
        SwingUtilities.invokeLater(() -> buildAndShow(connected, room));
    }

    private static void buildAndShow(GameClient client, String roomId) {
        DrawPanel panel = new DrawPanel();
        DrawController.init(client, panel);
        DrawController.setRoomId(roomId);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        addColorButton(toolBar, panel, "黒", "BLACK");
        addColorButton(toolBar, panel, "赤", "RED");
        addColorButton(toolBar, panel, "青", "BLUE");
        addColorButton(toolBar, panel, "緑", "GREEN");
        addColorButton(toolBar, panel, "消しゴム", "WHITE");

        JButton clearButton = new JButton("全消し");
        clearButton.addActionListener(e -> panel.clearCanvas());
        toolBar.add(clearButton);

        JFrame frame = new JFrame("お絵描きの森 - お絵描き (room: " + roomId + ")");
        frame.setLayout(new BorderLayout());
        frame.add(toolBar, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private static void addColorButton(JToolBar bar, DrawPanel panel, String label, String colorName) {
        JButton button = new JButton(label);
        button.addActionListener(e -> panel.setColorName(colorName));
        bar.add(button);
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
