package client.room;

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import client.GameClient;
import client.draw.DrawController;
import common.Protocol;

public class RoomController {
    private static GameClient client;
    private static RoomPanel panel;
    private static String currentRoom;
    private static String userName = "Player";

    public static void init(GameClient gameClient, RoomPanel roomPanel) {
        client = gameClient;
        panel = roomPanel;
    }

    public static void createRoom(String roomName, String requestedUserName) {
        if (!canSend()) {
            return;
        }

        String room = clean(roomName);
        String name = rememberUserName(requestedUserName);
        if (room.isEmpty()) {
            showStatus("部屋名を入力してください");
            return;
        }

        client.sendMessage(Protocol.ROOM_CREATE + ":" + room + "," + name);
    }

    public static void joinRoom(String roomName, String requestedUserName) {
        if (!canSend()) {
            return;
        }

        String room = clean(roomName);
        String name = rememberUserName(requestedUserName);
        if (room.isEmpty()) {
            showStatus("参加する部屋を選んでください");
            return;
        }

        client.sendMessage(Protocol.ROOM_JOIN + ":" + room + "," + name);
    }

    public static void requestRoomList() {
        if (!canSend()) {
            return;
        }
        client.sendMessage(Protocol.ROOM_LIST_REQUEST + ":");
    }

    public static void startGame() {
        if (!canSend()) {
            return;
        }
        if (currentRoom == null || currentRoom.isEmpty()) {
            showStatus("先に部屋を作成または参加してください");
            return;
        }
        client.sendMessage(Protocol.GAME_START + ":" + currentRoom);
    }

    public static String getCurrentRoom() {
        return currentRoom;
    }

    public static void onRoomUpdate(String data) {
        onRoomMessage(Protocol.ROOM_CREATED_NOTIFY, data);
    }

    public static void onRoomMessage(String command, String data) {
        switch (command) {
            case Protocol.ROOM_CREATED_NOTIFY:
                handleRoomAccepted(data, "部屋を作成しました");
                break;
            case Protocol.ROOM_JOINED_NOTIFY:
                handleRoomAccepted(data, "部屋に参加しました");
                break;
            case Protocol.ROOM_LIST:
                List<RoomInfo> rooms = parseRoomList(data);
                runOnUi(() -> {
                    if (panel != null) {
                        panel.setRooms(rooms);
                    }
                });
                break;
            case Protocol.ROOM_MEMBERS:
                handleMembers(data);
                break;
            case Protocol.ROOM_ERROR:
                showStatus(data);
                break;
            default:
                System.out.println("[RoomController] 未対応のコマンド: " + command + ":" + data);
        }
    }

    private static boolean canSend() {
        if (client == null) {
            showStatus("サーバーに接続されていません");
            return false;
        }
        return true;
    }

    private static String rememberUserName(String requestedUserName) {
        String cleaned = clean(requestedUserName);
        if (!cleaned.isEmpty()) {
            userName = cleaned;
        }
        return userName;
    }

    private static void handleRoomAccepted(String data, String statusPrefix) {
        String room = parseRoomName(data);
        if (room.isEmpty()) {
            showStatus("サーバーから部屋名が返ってきませんでした");
            return;
        }

        currentRoom = room;
        DrawController.setRoomId(room);
        runOnUi(() -> {
            if (panel != null) {
                panel.setCurrentRoom(room);
                panel.showStatus(statusPrefix + ": " + room);
            }
        });
    }

    private static void handleMembers(String data) {
        String[] parts = data == null ? new String[0] : data.split(",", 2);
        String room = parts.length > 0 ? parts[0].trim() : "";
        String memberData = parts.length > 1 ? parts[1].trim() : "";
        List<String> members = new ArrayList<>();

        if (!memberData.isEmpty()) {
            String[] names = memberData.split("\\|");
            for (String name : names) {
                String cleaned = clean(name);
                if (!cleaned.isEmpty()) {
                    members.add(cleaned);
                }
            }
        }

        if (!room.isEmpty()) {
            currentRoom = room;
            DrawController.setRoomId(room);
        }

        runOnUi(() -> {
            if (panel != null) {
                if (!room.isEmpty()) {
                    panel.setCurrentRoom(room);
                }
                panel.setMembers(members);
            }
        });
    }

    private static List<RoomInfo> parseRoomList(String data) {
        List<RoomInfo> rooms = new ArrayList<>();
        String text = data == null ? "" : data.trim();
        if (text.isEmpty()) {
            return rooms;
        }

        String[] entries = text.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(",", 2);
            String roomName = parts.length > 0 ? parts[0].trim() : "";
            int memberCount = 0;
            if (parts.length > 1) {
                try {
                    memberCount = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    memberCount = 0;
                }
            }
            if (!roomName.isEmpty()) {
                rooms.add(new RoomInfo(roomName, memberCount));
            }
        }
        return rooms;
    }

    private static String parseRoomName(String data) {
        if (data == null) {
            return "";
        }
        String[] parts = data.split(",", 2);
        return clean(parts[0]);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static void showStatus(String message) {
        runOnUi(() -> {
            if (panel != null) {
                panel.showStatus(message == null ? "" : message);
            } else {
                System.out.println("[RoomController] " + message);
            }
        });
    }

    private static void runOnUi(Runnable runnable) {
        SwingUtilities.invokeLater(runnable);
    }

    public static class RoomInfo {
        private final String name;
        private final int memberCount;

        public RoomInfo(String name, int memberCount) {
            this.name = name;
            this.memberCount = memberCount;
        }

        public String getName() {
            return name;
        }

        public int getMemberCount() {
            return memberCount;
        }

        @Override
        public String toString() {
            return name + " (" + memberCount + "人)";
        }
    }
}
