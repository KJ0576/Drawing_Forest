package server.room;

import server.ClientHandler;

public class RoomManager {
    public static void handleCreateRoom(ClientHandler client, String data) {
        // Aさんの処理：部屋を作って、作成完了通知（Protocol.ROOM_CREATED_NOTIFYなど）を client.sendMessage() で返す
    }
    public static void handleJoinRoom(ClientHandler client, String data) {
        // Aさんの処理：既存の部屋にプレイヤーを追加する
    }
}