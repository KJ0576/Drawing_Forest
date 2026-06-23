package common;

import java.util.Random;

public class WordManager {
    // お題のリスト（後からみんなで追加できるようにしておく）
    private static final String[] WORDS = {
        "りんご", "犬", "自動車", "プログラミング", "早稲田", "ポーカー"
    };

    private static final Random random = new Random();

    // ランダムにお題を1つ返すメソッド
    public static String getRandomWord() {
        int index = random.nextInt(WORDS.length);
        return WORDS[index];
    }
}