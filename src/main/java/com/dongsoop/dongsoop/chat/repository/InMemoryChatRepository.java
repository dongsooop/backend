package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Repository
public class InMemoryChatRepository implements ChatRepository {

    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessage>> messages = new ConcurrentHashMap<>();

    @Override
    public ChatRoom saveRoom(ChatRoom room) {
        rooms.put(room.getRoomId(), room);
        ensureMessageListExists(room.getRoomId());
        return room;
    }

    @Override
    public Optional<ChatRoom> findRoomById(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    @Override
    public Optional<ChatRoom> findRoomByParticipants(Long user1, Long user2) {
        return rooms.values().stream()
                .filter(hasExactlyTwoParticipants())
                .filter(containsBothUsers(user1, user2))
                .findFirst();
    }

    @Override
    public void saveMessage(ChatMessage message) {
        List<ChatMessage> roomMessages = ensureMessageListExists(message.getRoomId());
        roomMessages.add(message);
    }

    @Override
    public List<ChatMessage> findMessagesByRoomId(String roomId) {
        return new ArrayList<>(messages.getOrDefault(roomId, Collections.emptyList()));
    }

    private List<ChatMessage> ensureMessageListExists(String roomId) {
        return messages.computeIfAbsent(
                roomId,
                key -> Collections.synchronizedList(new ArrayList<>())
        );
    }

    private Predicate<ChatRoom> hasExactlyTwoParticipants() {
        return room -> room.getParticipants().size() == 2;
    }

    private Predicate<ChatRoom> containsBothUsers(Long user1, Long user2) {
        return room -> room.getParticipants().contains(user1)
                && room.getParticipants().contains(user2);
    }

    @Override
    public List<ChatRoom> findRoomsByUserId(Long userId) {
        return rooms.values().stream()
                .filter(room -> room.getParticipants().contains(userId))
                .toList();
    }
}