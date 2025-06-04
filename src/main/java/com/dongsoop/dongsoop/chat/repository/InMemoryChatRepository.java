package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Repository
public class InMemoryChatRepository implements ChatRepository {
    private static final int ONE_TO_ONE_PARTICIPANT_COUNT = 2;

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
                .filter(createOneToOneRoomFilter())
                .filter(createParticipantMatchFilter(user1, user2))
                .findFirst();
    }

    @Override
    public void saveMessage(ChatMessage message) {
        List<ChatMessage> roomMessages = ensureMessageListExists(message.getRoomId());
        roomMessages.add(message);
    }

    @Override
    public List<ChatMessage> findMessagesByRoomId(String roomId) {
        return createMessagesCopy(roomId);
    }

    @Override
    public List<ChatRoom> findRoomsByUserId(Long userId) {
        return rooms.values().stream()
                .filter(createUserParticipationFilter(userId))
                .toList();
    }

    private List<ChatMessage> ensureMessageListExists(String roomId) {
        return messages.computeIfAbsent(
                roomId,
                key -> Collections.synchronizedList(new ArrayList<>())
        );
    }

    private List<ChatMessage> createMessagesCopy(String roomId) {
        return new ArrayList<>(messages.getOrDefault(roomId, Collections.emptyList()));
    }

    private Predicate<ChatRoom> createOneToOneRoomFilter() {
        return room -> room.getParticipants().size() == ONE_TO_ONE_PARTICIPANT_COUNT;
    }

    private Predicate<ChatRoom> createParticipantMatchFilter(Long user1, Long user2) {
        return room -> {
            Set<Long> participants = room.getParticipants();
            return participants.contains(user1) && participants.contains(user2);
        };
    }

    private Predicate<ChatRoom> createUserParticipationFilter(Long userId) {
        return room -> room.getParticipants().contains(userId);
    }
}