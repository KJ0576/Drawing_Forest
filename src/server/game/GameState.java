package server.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GameState {
    private Theme currentTheme;
    private String drawerName;
    private long roundStartTime;

    private final Set<String> correctUsers = new HashSet<>();
    private final Map<String, Integer> scores = new HashMap<>();

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setCurrentTheme(Theme currentTheme) {
        this.currentTheme = currentTheme;
    }

    public String getDrawerName() {
        return drawerName;
    }

    public void setDrawerName(String drawerName) {
        this.drawerName = drawerName;
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(long roundStartTime) {
        this.roundStartTime = roundStartTime;
    }

    public boolean hasCorrect(String userName) {
        return correctUsers.contains(userName);
    }

    public void addCorrectUser(String userName) {
        correctUsers.add(userName);
    }

    public void addScore(String userName, int point) {
        scores.put(userName, scores.getOrDefault(userName, 0) + point);
    }

    public void initScore(String userName) {
        scores.putIfAbsent(userName, 0);
    }

    public String buildScoreText() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }

        return sb.toString();
    }
}
