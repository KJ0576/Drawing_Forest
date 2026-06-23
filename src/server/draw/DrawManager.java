package server.draw;

import server.ClientHandler;

public class DrawManager {
    public static void handleDrawData(ClientHandler client, String data) {
        // Bさんの処理：送られてきた座標データを、同じ部屋にいる他のプレイヤー（ClientHandlerたち）にそのまま sendMessage() で転送する
    }
}