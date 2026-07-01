package server.game;

import java.util.List;

public class Theme {
    private final String displayName;
    private final List<String> acceptedAnswers;

    public Theme(String displayName, List<String> acceptedAnswers) {
        this.displayName = displayName;
        this.acceptedAnswers = acceptedAnswers;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCorrect(String answer) {
        String normalizedAnswer = normalize(answer);

        for (String accepted : acceptedAnswers) {
            if (normalizedAnswer.equals(normalize(accepted))) {
                return true;
            }
        }

        return false;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.trim()
                .replace(" ", "")
                .replace("　", "")
                .toLowerCase();
    }
}
