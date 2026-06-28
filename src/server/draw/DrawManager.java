package server.draw;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import common.Protocol;
import server.ClientHandler;

/**
 * 担当B（お絵描き）のサーバー側処理。
 *
 * 役割：ある人が描いた座標データを ClientHandler から受け取り、
 *       「同じ部屋にいる他のプレイヤー」全員に転送（ブロードキャスト）する中継地点。
 *
 * ＜部屋メンバーの管理について＞
 *   本来「誰がどの部屋にいるか」は担当A(RoomManager)の管轄です。
 *   そこで、Aが部屋への入退室を扱うときに呼べるように joinRoom()/leaveRoom() を
 *   公開APIとして用意しています（連携ポイント）。
 *   まだAが未連携でも単体テストできるよう、描画データを送ってきた本人は
 *   handleDrawData() 内で自動的にその部屋へ登録します。
 */
public class DrawManager {

    // 部屋ID -> その部屋にいるクライアント集合（スレッドセーフ）
    private static final Map<String, Set<ClientHandler>> roomMembers = new ConcurrentHashMap<>();

    // ============================================================
    // 担当A(RoomManager)向けの連携API
    //   プレイヤーが部屋に入る/出るときに呼んでもらう想定。
    // ============================================================
    public static void joinRoom(String roomId, ClientHandler client) {
        roomMembers.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(client);
    }

    public static void leaveRoom(String roomId, ClientHandler client) {
        Set<ClientHandler> members = roomMembers.get(roomId);
        if (members != null) {
            members.remove(client);
            if (members.isEmpty()) {
                roomMembers.remove(roomId);
            }
        }
    }

    /** 切断時などに、このクライアントを全部屋から取り除く。 */
    public static void removeClient(ClientHandler client) {
        for (Set<ClientHandler> members : roomMembers.values()) {
            members.remove(client);
        }
    }

    // ============================================================
    // ClientHandler から呼ばれる本体
    //   data の形式: "部屋ID,X1,Y1,X2,Y2,色"   (例: "testroom,150,200,152,205,BLACK")
    // ============================================================
    public static void handleDrawData(ClientHandler client, String data) {
        // 先頭の「部屋ID」と、それ以降の「座標+色」に分ける
        String[] parts = data.split(",", 2);
        if (parts.length < 2) {
            System.out.println("[DrawManager] 不正な描画データを無視: " + data);
            return;
        }
        String roomId  = parts[0];
        String payload = parts[1]; // "X1,Y1,X2,Y2,色"

        // Aの連携がまだでも動くように、描いた本人を自動でその部屋に登録しておく
        joinRoom(roomId, client);

        Set<ClientHandler> members = roomMembers.get(roomId);
        if (members == null) {
            return;
        }

        // 受信側に渡すメッセージ（部屋IDは受信側では不要なので外す）
        String message = Protocol.DRAW_RECEIVED + ":" + payload;

        // 同じ部屋の「自分以外」全員へ転送
        for (ClientHandler member : members) {
            if (member != client) {
                member.sendMessage(message);
            }
        }
    }
}
