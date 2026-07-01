package server;

import java.io.*;
import java.net.*;
import common.Protocol;
// 以下の3つは各メンバーが作成するクラス
import server.room.RoomManager;
import server.draw.DrawManager;
import server.game.GameManager;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String userName = "Anonymous"; // プレイヤー名の保持用（必要に応じて）

    // コンストラクタ：接続されたソケットを受け取り、ストリームを準備する
    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            // 先生のプログラム例のストリーム作成部分を踏襲
            this.in = new BufferedReader(
                            new InputStreamReader(
                                socket.getInputStream()));
            this.out = new PrintWriter(
                             new BufferedWriter(
                                 new OutputStreamWriter(
                                     socket.getOutputStream())), true);
        } catch (IOException e) {
            System.err.println("ストリームの初期化に失敗しました: " + e.getMessage());
        }
    }

    // 先生のプログラム例の「while(true)で読み込むループ」をスレッドとして実行
    @Override
    public void run() {
        System.out.println("Connection accepted: " + socket);
        try {
            String message;
            // 接続が切れるか、"END"が来るまで常時受信を続ける
            while ((message = in.readLine()) != null) {
                if (message.equals("END")) {
                    break;
                }

                System.out.println("Received: " + message);

                // 電文の解析 (例: "ROOM_CREATE:my_room_name")
                String[] parts = message.split(":", 2);
                String command = parts[0];
                String data = (parts.length > 1) ? parts[1] : "";

                // 【合意事項の例外】A, B, Cの処理を仕分けるswitch文
                switch (command) {
                    // ==========================================
                    // Aさんの領域 (ルーム作成・参加)
                    // ==========================================
                    case Protocol.ROOM_CREATE:
                        RoomManager.handleCreateRoom(this, data);
                        break;
                    case Protocol.ROOM_JOIN:
                        RoomManager.handleJoinRoom(this, data);
                        break;
                    case Protocol.ROOM_LIST_REQUEST:
                        RoomManager.handleRoomListRequest(this);
                        break;

                    // ==========================================
                    // Bさんの領域 (お絵描き座標)
                    // ==========================================
                    case Protocol.DRAW_DATA:
                        DrawManager.handleDrawData(this, data);
                        break;

                    // ==========================================
                    // Cさんの領域 (回答・判定・進行)
                    // ==========================================
                    case Protocol.GAME_START:
                        GameManager.handleGameStart(this, data);
                        break;
                    case Protocol.CHAT_SUBMIT:
                        GameManager.handleChatSubmit(this, data);
                        break;

                    default:
                        System.out.println("未知のコマンド: " + command);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("クライアントとの通信が切断されました: " + e.getMessage());
        } finally {
            // 先生のプログラム例の終了・クローズ処理を踏襲
            closeConnection();
        }
    }

    // サーバーからこのクライアントに対してメッセージを送るための共通メソッド
    // A, B, Cさんが各自のロジックから結果を返送するときに使ってもらう
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    // クローズ処理の共通化
    private void closeConnection() {
        System.out.println("closing...");
        RoomManager.removeClient(this);
        DrawManager.removeClient(this);
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // プレイヤー名を保持・取得するアクセサ（Cさんのスコア管理などで便利）
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
