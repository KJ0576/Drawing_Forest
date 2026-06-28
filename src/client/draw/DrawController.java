package client.draw;

import javax.swing.SwingUtilities;

import client.GameClient;
import common.Protocol;

/**
 * 担当B（お絵描き）のクライアント側コントローラ。
 *
 * 送信：DrawPanel で線を引いたら sendLine() が呼ばれ、
 *       "DRAW_DATA:部屋ID,X1,Y1,X2,Y2,色" の形に文字列化してサーバーへ送る。
 * 受信：GameClient の受信スレッドが onDrawReceived() を static で呼ぶ。
 *       受け取った文字列を数値に戻し、DrawPanel に描画させる。
 *
 * GameClient から static で呼ばれる作りに合わせ、状態は static で保持する。
 */
public class DrawController {

    private static GameClient client;
    private static DrawPanel panel;
    // 部屋が確定するまでの暫定値。担当A側で部屋が決まったら setRoomId() で上書きする。
    private static String roomId = "default";

    /** 起動時に通信窓口(GameClient)と描画パネル(DrawPanel)を結びつける。 */
    public static void init(GameClient client, DrawPanel panel) {
        DrawController.client = client;
        DrawController.panel = panel;
    }

    /** 担当A(ルーム)で部屋が確定したら、その部屋IDをここに設定してもらう。 */
    public static void setRoomId(String id) {
        if (id != null && !id.isEmpty()) {
            roomId = id;
        }
    }

    public static String getRoomId() {
        return roomId;
    }

    // ============================================================
    // 送信：DrawPanel から「(x1,y1)→(x2,y2) に color で線を引いた」と通知される
    // ============================================================
    public static void sendLine(int x1, int y1, int x2, int y2, String color) {
        if (client == null) {
            return; // 未接続（描画のローカル確認のみ）の場合は送らない
        }
        String message = Protocol.DRAW_DATA + ":"
                + roomId + "," + x1 + "," + y1 + "," + x2 + "," + y2 + "," + color;
        client.sendMessage(message);
    }

    // ============================================================
    // 受信：GameClient の受信スレッドから呼ばれる
    //   data の形式: "X1,Y1,X2,Y2,色"   (例: "150,200,152,205,BLACK")
    // ============================================================
    public static void onDrawReceived(String data) {
        if (panel == null) {
            return;
        }
        try {
            String[] p = data.split(",");
            int x1 = Integer.parseInt(p[0].trim());
            int y1 = Integer.parseInt(p[1].trim());
            int x2 = Integer.parseInt(p[2].trim());
            int y2 = Integer.parseInt(p[3].trim());
            String color = (p.length > 4) ? p[4].trim() : "BLACK";

            // 受信スレッドからGUIを触らないよう、描画はEDT(イベントディスパッチスレッド)で行う
            SwingUtilities.invokeLater(() -> panel.drawRemoteLine(x1, y1, x2, y2, color));
        } catch (Exception e) {
            System.out.println("[DrawController] 受信データの解析に失敗: " + data);
        }
    }
}
