package server.game;

import server.ClientHandler;

public class GameManager {
    public static void handleGameStart(ClientHandler client, String data) {
        // Cさんの処理：ゲーム開始フラグを立てて、お題を選び、役割（描く人・当てる人）を通知する
    }
    public static void handleChatSubmit(ClientHandler client, String data) {
        // Cさんの処理：届いたチャットがお題（正解）と一致するか判定。
        // 正解ならスコア加算して全員に通知。ハズレなら通常のチャットとして全員に転送。
    }
}