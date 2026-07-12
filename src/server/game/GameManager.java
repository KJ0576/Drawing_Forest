package server.game;

import common.Protocol;
import server.ClientHandler;
import server.room.RoomManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class GameManager {

    // 1ラウンドの制限時間
    private static final int ROUND_TIME_SECONDS = 60;

    // 部屋IDごとに「現在のお題」を保存する
    private static final Map<String, String> currentThemes =
            new HashMap<>();

    // 部屋IDごとに「現在の描く人」を保存する
    private static final Map<String, String> drawersByRoom =
            new HashMap<>();

    // 部屋IDごとに「すでに正解したユーザー」を保存する
    private static final Map<String, Set<String>> correctUsersByRoom =
            new HashMap<>();

    // 部屋IDごとに「ユーザーごとのスコア」を保存する
    private static final Map<String, Map<String, Integer>> scoresByRoom =
            new HashMap<>();

    // 部屋IDごとに描く人の順番を保存する
    private static final Map<String, List<ClientHandler>> drawerOrderByRoom =
            new HashMap<>();

    // 部屋IDごとに現在の描く人の順番を保存する
    private static final Map<String, Integer> drawerIndexByRoom =
            new HashMap<>();

    // 部屋IDごとにタイマーを保存する
    private static final Map<String, Timer> timersByRoom =
            new HashMap<>();

    // ラウンド終了の重複実行を防ぐ
    private static final Set<String> endingRooms =
            new HashSet<>();

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

    public static synchronized void handleGameStart(
            ClientHandler client,
            String data
    ) {
        // dataには部屋IDが入っている想定
        String roomId = data.trim();

        if (roomId.isEmpty()) {
            client.sendMessage(
                    Protocol.ROOM_ERROR + ":部屋IDが空です"
            );
            return;
        }

        List<ClientHandler> members =
                RoomManager.getRoomMembers(roomId);

        if (members == null || members.size() < 2) {
            client.sendMessage(
                    Protocol.ROOM_ERROR
                            + ":ゲーム開始には2人以上必要です"
            );
            return;
        }

        // 指定された部屋に送信者が所属しているか確認
        if (!members.contains(client)) {
            client.sendMessage(
                    Protocol.ROOM_ERROR
                            + ":この部屋に参加していません"
            );
            return;
        }

        // すでにゲームが開始されている場合は重複開始させない
        if (drawerOrderByRoom.containsKey(roomId)) {
            client.sendMessage(
                    Protocol.ROOM_ERROR
                            + ":ゲームはすでに開始されています"
            );
            return;
        }

        // スコア表を初期化
        Map<String, Integer> roomScores = new HashMap<>();

        for (ClientHandler member : members) {
            roomScores.put(member.getUserName(), 0);
        }

        scoresByRoom.put(roomId, roomScores);

        // 全員が1回ずつ描くように順番を作る
        List<ClientHandler> drawerOrder =
                new ArrayList<>(members);

        Collections.shuffle(drawerOrder);

        drawerOrderByRoom.put(roomId, drawerOrder);
        drawerIndexByRoom.put(roomId, 0);

        // 最初のラウンドを開始
        startRound(roomId);
    }

    private static synchronized void startRound(String roomId) {
        List<ClientHandler> drawerOrder =
                drawerOrderByRoom.get(roomId);

        Integer drawerIndex =
                drawerIndexByRoom.get(roomId);

        if (drawerOrder == null || drawerIndex == null) {
            return;
        }

        // 全員が描き終わったらゲーム終了
        if (drawerIndex >= drawerOrder.size()) {
            endGame(roomId);
            return;
        }

        List<ClientHandler> currentMembers =
                RoomManager.getRoomMembers(roomId);

        if (currentMembers == null || currentMembers.size() < 2) {
            endGame(roomId);
            return;
        }

        ClientHandler drawer = drawerOrder.get(drawerIndex);

        // 描く人が途中で退出していた場合は次へ進む
        if (!currentMembers.contains(drawer)) {
            drawerIndexByRoom.put(roomId, drawerIndex + 1);
            startRound(roomId);
            return;
        }

        String theme =
                themes.get(random.nextInt(themes.size()));

        String drawerName = drawer.getUserName();

        currentThemes.put(roomId, theme);
        drawersByRoom.put(roomId, drawerName);
        correctUsersByRoom.put(roomId, new HashSet<>());
        endingRooms.remove(roomId);

        Map<String, Integer> roomScores =
                scoresByRoom.get(roomId);

        if (roomScores == null) {
            roomScores = new HashMap<>();
            scoresByRoom.put(roomId, roomScores);
        }

        for (ClientHandler member : currentMembers) {
            roomScores.putIfAbsent(
                    member.getUserName(),
                    0
            );
        }

        // 描く人にだけお題を送る
        drawer.sendMessage(
                Protocol.GAME_ROUND_START
                        + ":DRAWER,"
                        + theme
        );

        // 当てる人には描く人の名前だけを送る
        for (ClientHandler member : currentMembers) {
            if (member != drawer) {
                member.sendMessage(
                        Protocol.GAME_ROUND_START
                                + ":GUESSER,"
                                + drawerName
                );
            }
        }

        // 全員に現在のスコアを送る
        RoomManager.broadcastToRoom(
                roomId,
                Protocol.GAME_SCORE_UPDATE
                        + ":"
                        + buildScoreText(roomScores)
        );

        // 制限時間の計測を開始
        startRoundTimer(roomId);
    }

    public static synchronized void handleChatSubmit(
            ClientHandler client,
            String data
    ) {
        // dataには「部屋ID,発言内容」が入っている想定
        String[] parts = data.split(",", 2);

        if (parts.length < 2) {
            client.sendMessage(
                    Protocol.ROOM_ERROR
                            + ":回答形式が不正です"
            );
            return;
        }

        String roomId = parts[0].trim();
        String answer = parts[1].trim();

        if (roomId.isEmpty()) {
            client.sendMessage(
                    Protocol.ROOM_ERROR
                            + ":部屋IDが空です"
            );
            return;
        }

        if (answer.isEmpty()) {
            client.sendMessage(
                    Protocol.ROOM_ERROR
                            + ":回答が空です"
            );
            return;
        }

        List<ClientHandler> members =
                RoomManager.getRoomMembers(roomId);

        if (members == null || !members.contains(client)) {
            client.sendMessage(
                    Protocol.ROOM_ERROR
                            + ":この部屋に参加していません"
            );
            return;
        }

        String currentTheme =
                currentThemes.get(roomId);

        if (currentTheme == null) {
            client.sendMessage(
                    Protocol.ROOM_ERROR
                            + ":ゲームが開始されていません"
            );
            return;
        }

        String userName = client.getUserName();
        String drawerName = drawersByRoom.get(roomId);

        // 描く人の発言は通常チャットとして共有
        if (userName.equals(drawerName)) {
            RoomManager.broadcastToRoom(
                    roomId,
                    Protocol.CHAT_BROADCAST
                            + ":"
                            + userName
                            + ","
                            + answer
            );
            return;
        }

        Set<String> correctUsers =
                correctUsersByRoom.get(roomId);

        if (correctUsers == null) {
            correctUsers = new HashSet<>();
            correctUsersByRoom.put(
                    roomId,
                    correctUsers
            );
        }

        // すでに正解した人には二重加点しない
        if (correctUsers.contains(userName)) {
            RoomManager.broadcastToRoom(
                    roomId,
                    Protocol.CHAT_BROADCAST
                            + ":"
                            + userName
                            + ","
                            + answer
            );

            client.sendMessage(
                    Protocol.GAME_JUDGE_RESULT
                            + ":"
                            + userName
                            + ",ALREADY_CORRECT,"
                            + answer
            );
            return;
        }

        boolean isCorrect =
                normalize(answer).equals(
                        normalize(currentTheme)
                );

        if (isCorrect) {
            correctUsers.add(userName);

            Map<String, Integer> roomScores =
                    scoresByRoom.get(roomId);

            if (roomScores == null) {
                roomScores = new HashMap<>();
                scoresByRoom.put(
                        roomId,
                        roomScores
                );
            }

            int currentScore =
                    roomScores.getOrDefault(
                            userName,
                            0
                    );

            // 正解した人に100点加算
            roomScores.put(
                    userName,
                    currentScore + 100
            );

            /*
             * 正解の回答内容は全員に表示しない。
             * お題が他の回答者に漏れるのを防ぐ。
             */
            RoomManager.broadcastToRoom(
                    roomId,
                    Protocol.GAME_JUDGE_RESULT
                            + ":"
                            + userName
                            + ",CORRECT,"
            );

            RoomManager.broadcastToRoom(
                    roomId,
                    Protocol.GAME_SCORE_UPDATE
                            + ":"
                            + buildScoreText(roomScores)
            );

            if (isAllGuessersCorrect(roomId)) {
                finishRound(roomId);
            }

        } else {
            // 不正解の発言はチャット欄に表示
            RoomManager.broadcastToRoom(
                    roomId,
                    Protocol.CHAT_BROADCAST
                            + ":"
                            + userName
                            + ","
                            + answer
            );

            RoomManager.broadcastToRoom(
                    roomId,
                    Protocol.GAME_JUDGE_RESULT
                            + ":"
                            + userName
                            + ",WRONG,"
                            + answer
            );
        }
    }

    private static synchronized void startRoundTimer(
            String roomId
    ) {
        cancelTimer(roomId);

        Timer timer = new Timer(true);
        timersByRoom.put(roomId, timer);

        for (int elapsed = 0;
             elapsed <= ROUND_TIME_SECONDS;
             elapsed++) {

            final int remaining =
                    ROUND_TIME_SECONDS - elapsed;

            timer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            synchronized (GameManager.class) {
                                if (!currentThemes.containsKey(roomId)) {
                                    cancel();
                                    return;
                                }

                                RoomManager.broadcastToRoom(
                                        roomId,
                                        Protocol.GAME_TIME_UPDATE
                                                + ":"
                                                + remaining
                                );

                                if (remaining == 0) {
                                    finishRound(roomId);
                                }
                            }
                        }
                    },
                    elapsed * 1000L
            );
        }
    }

    private static synchronized void finishRound(
            String roomId
    ) {
        // 正解と時間切れが同時発生しても1回だけ終了する
        if (endingRooms.contains(roomId)) {
            return;
        }

        String currentTheme =
                currentThemes.get(roomId);

        if (currentTheme == null) {
            return;
        }

        endingRooms.add(roomId);
        cancelTimer(roomId);

        RoomManager.broadcastToRoom(
                roomId,
                Protocol.GAME_ROUND_END
                        + ":"
                        + currentTheme
        );

        currentThemes.remove(roomId);
        drawersByRoom.remove(roomId);
        correctUsersByRoom.remove(roomId);

        int nextIndex =
                drawerIndexByRoom.getOrDefault(
                        roomId,
                        0
                ) + 1;

        drawerIndexByRoom.put(
                roomId,
                nextIndex
        );

        /*
         * 終了結果を画面に表示する時間を設けてから
         * 次のラウンドを開始する。
         */
        Timer nextRoundTimer = new Timer(true);

        nextRoundTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (GameManager.class) {
                            endingRooms.remove(roomId);
                            startRound(roomId);
                        }
                    }
                },
                2000L
        );
    }

    private static synchronized void endGame(
            String roomId
    ) {
        cancelTimer(roomId);

        Map<String, Integer> roomScores =
                scoresByRoom.get(roomId);

        String finalScores =
                roomScores == null
                        ? ""
                        : buildScoreText(roomScores);

        RoomManager.broadcastToRoom(
                roomId,
                Protocol.GAME_END
                        + ":"
                        + finalScores
        );

        currentThemes.remove(roomId);
        drawersByRoom.remove(roomId);
        correctUsersByRoom.remove(roomId);
        scoresByRoom.remove(roomId);
        drawerOrderByRoom.remove(roomId);
        drawerIndexByRoom.remove(roomId);
        endingRooms.remove(roomId);
    }

    private static synchronized void cancelTimer(
            String roomId
    ) {
        Timer timer = timersByRoom.remove(roomId);

        if (timer != null) {
            timer.cancel();
        }
    }

    private static boolean isAllGuessersCorrect(
            String roomId
    ) {
        List<ClientHandler> members =
                RoomManager.getRoomMembers(roomId);

        String drawerName =
                drawersByRoom.get(roomId);

        Set<String> correctUsers =
                correctUsersByRoom.get(roomId);

        if (members == null
                || drawerName == null
                || correctUsers == null) {
            return false;
        }

        int guesserCount = 0;

        for (ClientHandler member : members) {
            String memberName =
                    member.getUserName();

            if (!memberName.equals(drawerName)) {
                guesserCount++;
            }
        }

        return guesserCount > 0
                && correctUsers.size() >= guesserCount;
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

    private static String buildScoreText(
            Map<String, Integer> roomScores
    ) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Integer> entry
                : roomScores.entrySet()) {

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
