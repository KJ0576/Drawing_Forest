package client;

import java.io.*;
import java.net.*;
import common.Protocol;
import client.room.RoomController;
import client.draw.DrawController;
import client.game.GameController;

public class GameClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // サーバーに接続する
    public void connect(String serverAddress, int port) throws IOException {
        socket = new Socket(serverAddress, port);
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // サーバーからのメッセージを常時待ち受けるスレッドを開始
        startListening();
    }

    // サーバーへメッセージを送信するメソッド（A,B,CのControllerから呼ばれる）
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // 受信専用のバックグラウンド処理
    private void startListening() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    // 受信したデータをコマンドと中身に分ける
                    String[] parts = message.split(":", 2);
                    String command = parts[0];
                    String data = (parts.length > 1) ? parts[1] : "";

                    // 【クライアント側のswitch文】A, B, CのControllerへ分配
                    switch (command) {
                        case Protocol.ROOM_CREATED_NOTIFY:
                        case Protocol.ROOM_JOINED_NOTIFY:
                        case Protocol.ROOM_LIST:
                        case Protocol.ROOM_MEMBERS:
                        case Protocol.ROOM_ERROR:
                            RoomController.onRoomMessage(command, data);
                            break;
                        case Protocol.DRAW_RECEIVED:
                            DrawController.onDrawReceived(data);
                            break;
                        case Protocol.GAME_ROUND_START:
                        case Protocol.GAME_SCORE_UPDATE:
                            GameController.onGameUpdate(command, data);
                            break;
                        default:
                            System.out.println("不明な受信: " + message);
                    }
                }
            } catch (IOException e) {
                System.out.println("サーバーから切断されました");
            }
        }).start();
    }
}
