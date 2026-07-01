package server.game;

import common.Protocol;
import server.ClientHandler;
import server.room.RoomManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class GameManager {

    // 部屋IDごとに「現在のお題」を保存する
    private static final Map<String, String> currentThemes = new HashMap<>();

    // 部屋IDごとに「現在の描く人」を保存する
    private static final Map<String, String> drawersByRoom = new HashMap<>();

    // 部屋IDごとに「すでに正解したユーザー」を保存する
    private static final Map<String, Set<String>> correctUsersByRoom = new HashMap<>();

    // 部屋IDごとに「ユーザーごとのスコア」を保存する
    private static final Map<String, Map<String, Integer>> scoresByRoom = new HashMap<>();

    // 出題するお題リスト
    private static final List<String> themes = Arrays.asList(
            "りんご",
            "ねこ",
            "いぬ",
            "くるま",
            "さかな",
            "バナナ",
            "時計",
            "学校",
            "飛行機",
            "パソコン"
    );

    // ランダムにお題・描く人を選ぶための部品
    private static final Random random = new Random();

    public static void handleGameStart(ClientHandler client, String data) {
        // dataには部屋IDが入っている想定
        // 例: GAME_START:room1 の場合、data = "room1"
        String roomId = data.trim();

        if (roomId.isEmpty()) {
            client.sendMessage(Protocol.ROOM_ERROR + ":部屋IDが空です");
            return;
        }

        List<ClientHandler> members = RoomManager.getRoomMembers(roomId);

        if (members.size() < 2) {
            client.sendMessage(Protocol.ROOM_ERROR + ":ゲーム開始には2人以上必要です");
            return;
        }

        // お題リストからランダムに1つ選ぶ
        String theme = themes.get(random.nextInt(themes.size()));

        // 部屋メンバーからランダムに描く人を1人選ぶ
        ClientHandler drawer = members.get(random.nextInt(members.size()));
        String drawerName = drawer.getUserName();

        // この部屋の現在のお題・描く人を保存する
        currentThemes.put(roomId, theme);
        drawersByRoom.put(roomId, drawerName);

        // この部屋の正解済みユーザーを初期化する
        correctUsersByRoom.put(roomId, new HashSet<>());

        // この部屋のスコア表を用意し、参加者を初期化する
        Map<String, Integer> roomScores = scoresByRoom.get(roomId);
        if (roomScores == null) {
            roomScores = new HashMap<>();
            scoresByRoom.put(roomId, roomScores);
        }

        for (ClientHandler member : members) {
            roomScores.putIfAbsent(member.getUserName(), 0);
        }

        // 描く人にはお題を送る
        drawer.sendMessage(Protocol.GAME_ROUND_START + ":DRAWER," + theme);

        // 当てる人にはお題を送らず、描く人の名前だけ送る
        for (ClientHandler member : members) {
            if (member != drawer) {
                member.sendMessage(Protocol.GAME_ROUND_START + ":GUESSER," + drawerName);
            }
        }

        // 全員にスコア初期状態を送る
        RoomManager.broadcastToRoom(
                roomId,
                Protocol.GAME_SCORE_UPDATE + ":" + buildScoreText(roomScores)
        );
    }

    public static void handleChatSubmit(ClientHandler client, String data) {
        // dataには「部屋ID,発言内容」が入っている想定
        // 例: CHAT_SUBMIT:room1,りんご の場合、data = "room1,りんご"
        String[] parts = data.split(",", 2);

        if (parts.length < 2) {
            client.sendMessage(Protocol.ROOM_ERROR + ":回答形式が不正です");
            return;
        }

        String roomId = parts[0].trim();
        String answer = parts[1].trim();

        if (roomId.isEmpty()) {
            client.sendMessage(Protocol.ROOM_ERROR + ":部屋IDが空です");
            return;
        }

        if (answer.isEmpty()) {
            client.sendMessage(Protocol.ROOM_ERROR + ":回答が空です");
            return;
        }

        // 現在のお題を取得する
        String currentTheme = currentThemes.get(roomId);

        if (currentTheme == null) {
            client.sendMessage(Protocol.ROOM_ERROR + ":ゲームが開始されていません");
            return;
        }

        String userName = client.getUserName();
        String drawerName = drawersByRoom.get(roomId);

        // 描く人の発言は回答として判定しない
        if (userName.equals(drawerName)) {
            return;
        }

        // この部屋の正解済みユーザー一覧を取得する
        Set<String> correctUsers = correctUsersByRoom.get(roomId);

        if (correctUsers == null) {
            correctUsers = new HashSet<>();
            correctUsersByRoom.put(roomId, correctUsers);
        }

        // すでに正解した人には二重加点しない
        if (correctUsers.contains(userName)) {
            RoomManager.broadcastToRoom(
                    roomId,
                    Protocol.GAME_JUDGE_RESULT + ":" + userName + ",ALREADY_CORRECT," + answer
            );
            return;
        }

        // 回答とお題を比較する
        boolean isCorrect = normalize(answer).equals(normalize(currentTheme));

        if (isCorrect) {
            // 正解済みユーザーとして登録
            correctUsers.add(userName);

            // スコア表を取得
            Map<String, Integer> roomScores = scoresByRoom.get(roomId);

            if (roomScores == null) {
                roomScores = new HashMap<>();
                scoresByRoom.put(roomId, roomScores);
            }

            // 正解した人に100点加算
            int currentScore = roomScores.getOrDefault(userName, 0);
            roomScores.put(userName, currentScore + 100);

            // 正解通知を同じ部屋の全員に送る
            RoomManager.broadcastToRoom(
                    roomId,
                    Protocol.GAME_JUDGE_RESULT + ":" + userName + ",CORRECT," + answer
            );

            // スコア更新を同じ部屋の全員に送る
            RoomManager.broadcastToRoom(
                    roomId,
                    Protocol.GAME_SCORE_UPDATE + ":" + buildScoreText(roomScores)
            );

            // 描く人以外が全員正解したらラウンド終了
            if (isAllGuessersCorrect(roomId)) {
                RoomManager.broadcastToRoom(
                        roomId,
                        Protocol.GAME_ROUND_END + ":" + currentTheme
                );

                currentThemes.remove(roomId);
                drawersByRoom.remove(roomId);
                correctUsersByRoom.remove(roomId);
            }

        } else {
            // 不正解通知を同じ部屋の全員に送る
            RoomManager.broadcastToRoom(
                    roomId,
                    Protocol.GAME_JUDGE_RESULT + ":" + userName + ",WRONG," + answer
            );
        }
    }

    private static boolean isAllGuessersCorrect(String roomId) {
        List<ClientHandler> members = RoomManager.getRoomMembers(roomId);
        String drawerName = drawersByRoom.get(roomId);
        Set<String> correctUsers = correctUsersByRoom.get(roomId);

        if (drawerName == null || correctUsers == null) {
            return false;
        }

        int guesserCount = 0;

        for (ClientHandler member : members) {
            String memberName = member.getUserName();

            if (!memberName.equals(drawerName)) {
                guesserCount++;
            }
        }

        return guesserCount > 0 && correctUsers.size() >= guesserCount;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.trim()
                .replace(" ", "")
                .replace("　", "")
                .toLowerCase();
    }

    private static String buildScoreText(Map<String, Integer> roomScores) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Integer> entry : roomScores.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }

            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }

        return sb.toString();
    }
}
