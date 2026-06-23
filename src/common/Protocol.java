package common;

public class Protocol {
    // A: ルーム関連コマンド
    public static final String ROOM_CREATE = "ROOM_CREATE";       // 引数: 部屋名
    public static final String ROOM_JOIN   = "ROOM_JOIN";         // 引数: 部屋ID, ユーザー名
    public static final String ROOM_CREATED_NOTIFY = "ROOM_R_NEW"; // サーバーからの返答
    
    // B: お絵描き関連コマンド
    public static final String DRAW_DATA   = "DRAW_DATA";         // 引数: 部屋ID, X1,Y1,X2,Y2,色
    public static final String DRAW_RECEIVED = "DRAW_RCV";        // サーバーからの他プレイヤーへの転送
    
    // C: ゲーム進行・判定関連コマンド
    public static final String GAME_START  = "GAME_START";        // 引数: 部屋ID
    public static final String GAME_ROUND_START = "G_R_START";    // サーバーから: 役割(役割コード, お題文字列※描く人のみ)
    public static final String CHAT_SUBMIT = "CHAT_SUBMIT";       // 引数: 部屋ID, 発言内容
    public static final String GAME_SCORE_UPDATE = "G_SCORE";     // サーバーから: スコアデータ一覧
}