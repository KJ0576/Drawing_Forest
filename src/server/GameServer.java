package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class GameServer {
    public static final int PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("サーバーが起動しました。ポート: " + PORT);

            // 無限ループで複数人の接続を待ち続ける
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("新しいプレイヤーが接続しました: " + clientSocket);

                // 接続してきた人専属のClientHandler（先ほど作ったクラス）を生成
                ClientHandler handler = new ClientHandler(clientSocket);
                
                // 別スレッドで動かすことで、次の人の接続をすぐに待てるようにする
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}