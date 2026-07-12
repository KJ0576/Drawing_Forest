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

    // 部屋IDごとに「ユーザーごとのス
