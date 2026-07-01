package server.room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import common.Protocol;
import server.ClientHandler;
import server.draw.DrawManager;

public class RoomManager {
    private static final int MAX_ROOM_NAME_LENGTH = 30;
    private static final int MAX_USER_NAME_LENGTH = 20;

    private static final Object roomLock = new Object();
    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final Map<ClientHandler, String> clientRooms = new ConcurrentHashMap<>();
    private static final Set<ClientHandler> knownClients = ConcurrentHashMap.newKeySet();

    public static void handleCreateRoom(ClientHandler client, String data) {
        knownClients.add(client);

        RoomRequest request = parseRoomRequest(data);
        String error = validateRoomName(request.roomName);
        if (error != null) {
            sendError(client, error);
            sendRoomList(client);
            return;
        }

        String userName = normalizeUserName(request.userName, client);
        String previousRoom = null;

        synchronized (roomLock) {
            if (rooms.containsKey(request.roomName)) {
                sendError(client, "同じ名前の部屋がすでにあります");
                sendRoomList(client);
                return;
            }

            Room room = new Room(request.roomName);
            rooms.put(request.roomName, room);
            previousRoom = moveClientToRoomLocked(client, room, userName);
        }

        client.sendMessage(Protocol.ROOM_CREATED_NOTIFY + ":" + request.roomName + ",成功");
        client.sendMessage(Protocol.ROOM_JOINED_NOTIFY + ":" + request.roomName + ",成功");
        if (previousRoom != null) {
            broadcastMembers(previousRoom);
        }
        broadcastMembers(request.roomName);
        broadcastRoomList();
    }

    public static void handleJoinRoom(ClientHandler client, String data) {
        knownClients.add(client);

        RoomRequest request = parseRoomRequest(data);
        String error = validateRoomName(request.roomName);
        if (error != null) {
            sendError(client, error);
            sendRoomList(client);
            return;
        }

        String userName = normalizeUserName(request.userName, client);
        String previousRoom;

        synchronized (roomLock) {
            Room room = rooms.get(request.roomName);
            if (room == null) {
                sendError(client, "指定された部屋がありません");
                sendRoomList(client);
                return;
            }
            previousRoom = moveClientToRoomLocked(client, room, userName);
        }

        client.sendMessage(Protocol.ROOM_JOINED_NOTIFY + ":" + request.roomName + ",成功");
        if (previousRoom != null) {
            broadcastMembers(previousRoom);
        }
        broadcastMembers(request.roomName);
        broadcastRoomList();
    }

    public static void handleRoomListRequest(ClientHandler client) {
        knownClients.add(client);
        sendRoomList(client);
    }

    public static void removeClient(ClientHandler client) {
        knownClients.remove(client);

        String oldRoomName;
        synchronized (roomLock) {
            oldRoomName = clientRooms.remove(client);
            if (oldRoomName != null) {
                Room oldRoom = rooms.get(oldRoomName);
                if (oldRoom != null) {
                    oldRoom.removeMember(client);
                    if (oldRoom.isEmpty()) {
                        rooms.remove(oldRoomName);
                    }
                }
                DrawManager.leaveRoom(oldRoomName, client);
            }
        }

        if (oldRoomName != null) {
            broadcastMembers(oldRoomName);
            broadcastRoomList();
        }
    }

    public static List<ClientHandler> getRoomMembers(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(room.members);
    }

    public static String getRoomOf(ClientHandler client) {
        return clientRooms.get(client);
    }

    private static String moveClientToRoomLocked(ClientHandler client, Room room, String userName) {
        client.setUserName(userName);

        String oldRoomName = clientRooms.get(client);
        if (oldRoomName != null && !oldRoomName.equals(room.name)) {
            Room oldRoom = rooms.get(oldRoomName);
            if (oldRoom != null) {
                oldRoom.removeMember(client);
                if (oldRoom.isEmpty()) {
                    rooms.remove(oldRoomName);
                }
            }
            DrawManager.leaveRoom(oldRoomName, client);
        } else {
            oldRoomName = null;
        }

        room.addMember(client);
        clientRooms.put(client, room.name);
        DrawManager.joinRoom(room.name, client);
        return oldRoomName;
    }

    private static RoomRequest parseRoomRequest(String data) {
        String[] parts = (data == null) ? new String[0] : data.split(",", 2);
        String roomName = parts.length > 0 ? parts[0].trim() : "";
        String userName = parts.length > 1 ? parts[1].trim() : "";
        return new RoomRequest(roomName, userName);
    }

    private static String validateRoomName(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            return "部屋名を入力してください";
        }
        if (roomName.length() > MAX_ROOM_NAME_LENGTH) {
            return "部屋名は" + MAX_ROOM_NAME_LENGTH + "文字以内にしてください";
        }
        if (hasProtocolDelimiter(roomName)) {
            return "部屋名に : , ; | は使えません";
        }
        return null;
    }

    private static String normalizeUserName(String requestedName, ClientHandler client) {
        String name = requestedName;
        if (name == null || name.trim().isEmpty()) {
            name = client.getUserName();
        }
        if (name == null || name.trim().isEmpty() || "Anonymous".equals(name)) {
            name = "Player";
        }

        name = name.trim();
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            normalized.append(isProtocolDelimiter(c) ? '_' : c);
        }

        String result = normalized.toString();
        if (result.length() > MAX_USER_NAME_LENGTH) {
            result = result.substring(0, MAX_USER_NAME_LENGTH);
        }
        return result;
    }

    private static boolean hasProtocolDelimiter(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (isProtocolDelimiter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProtocolDelimiter(char c) {
        return c == ':' || c == ',' || c == ';' || c == '|';
    }

    private static void sendError(ClientHandler client, String message) {
        client.sendMessage(Protocol.ROOM_ERROR + ":" + message);
    }

    private static void sendRoomList(ClientHandler client) {
        client.sendMessage(Protocol.ROOM_LIST + ":" + buildRoomListData());
    }

    private static void broadcastRoomList() {
        String message = Protocol.ROOM_LIST + ":" + buildRoomListData();
        for (ClientHandler client : knownClients) {
            client.sendMessage(message);
        }
    }

    private static String buildRoomListData() {
        List<Room> sortedRooms = new ArrayList<>(rooms.values());
        sortedRooms.sort(Comparator.comparing(room -> room.name));

        List<String> entries = new ArrayList<>();
        for (Room room : sortedRooms) {
            entries.add(room.name + "," + room.memberCount());
        }
        return String.join(";", entries);
    }

    private static void broadcastMembers(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            return;
        }

        String message = Protocol.ROOM_MEMBERS + ":" + roomName + "," + String.join("|", room.memberNames());
        for (ClientHandler member : room.members) {
            member.sendMessage(message);
        }
    }

    private static class Room {
        private final String name;
        private final CopyOnWriteArrayList<ClientHandler> members = new CopyOnWriteArrayList<>();

        private Room(String name) {
            this.name = name;
        }

        private void addMember(ClientHandler client) {
            if (!members.contains(client)) {
                members.add(client);
            }
        }

        private void removeMember(ClientHandler client) {
            members.remove(client);
        }

        private boolean isEmpty() {
            return members.isEmpty();
        }

        private int memberCount() {
            return members.size();
        }

        private List<String> memberNames() {
            List<String> names = new ArrayList<>();
            for (ClientHandler member : members) {
                names.add(member.getUserName());
            }
            return names;
        }
    }

    private static class RoomRequest {
        private final String roomName;
        private final String userName;

        private RoomRequest(String roomName, String userName) {
            this.roomName = roomName;
            this.userName = userName;
        }
    }
}
