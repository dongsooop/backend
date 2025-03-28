package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryChatRepository implements ChatRepository {

    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessage>> messages = new ConcurrentHashMap<>();

    @Override
    public ChatRoom saveRoom(ChatRoom room) {
        rooms.put(room.getRoomId(), room);
        messages.putIfAbsent(room.getRoomId(), Collections.synchronizedList(new ArrayList<>()));
        return room;
    }

    @Override
    public Optional<ChatRoom> findRoomById(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    @Override
    public Optional<ChatRoom> findRoomByParticipants(String user1, String user2) {
        return rooms.values().stream()
                .filter(room -> room.getParticipants().size() == 2)
                .filter(room -> room.getParticipants().contains(user1) && room.getParticipants().contains(user2))
                .findFirst();
    }

    @Override
    public void saveMessage(ChatMessage message) {
        List<ChatMessage> roomMessages = messages.computeIfAbsent(
                message.getRoomId(),
                k -> Collections.synchronizedList(new ArrayList<>())
        );
        roomMessages.add(message);
    }

    @Override
    public List<ChatMessage> findMessagesByRoomId(String roomId) {
        return new ArrayList<>(messages.getOrDefault(roomId, Collections.emptyList()));
    }
}