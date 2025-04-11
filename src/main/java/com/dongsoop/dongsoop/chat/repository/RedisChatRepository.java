package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Repository
public class RedisChatRepository implements ChatRepository {
    private static final long CHAT_TTL = 30; // 30일 TTL
    private static final String ROOM_KEY_PREFIX = "chat:room:";
    private static final String MESSAGE_KEY_PREFIX = "chat:message:";
    private static final String MESSAGE_LIST_PREFIX = "chat:messages:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisChatRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ChatRoom saveRoom(ChatRoom room) {
        String key = getRoomKey(room.getRoomId());
        saveWithExpiration(key, room);
        return room;
    }

    @Override
    public Optional<ChatRoom> findRoomById(String roomId) {
        String key = getRoomKey(roomId);
        return Optional.ofNullable((ChatRoom) redisTemplate.opsForValue().get(key));
    }

    @Override
    public Optional<ChatRoom> findRoomByParticipants(String user1, String user2) {
        return findRoomsWithFilter(room -> {
            Set<String> participants = room.getParticipants();
            return participants.size() == 2
                    && participants.contains(user1)
                    && participants.contains(user2);
        }).stream().findFirst();
    }

    @Override
    public void saveMessage(ChatMessage message) {
        String messageKey = getMessageKey(message.getRoomId(), message.getMessageId());
        String listKey = getMessageListKey(message.getRoomId());

        saveWithExpiration(messageKey, message);
        redisTemplate.opsForList().rightPush(listKey, message);
        redisTemplate.expire(listKey, CHAT_TTL, TimeUnit.DAYS);
    }

    @Override
    public List<ChatMessage> findMessagesByRoomId(String roomId) {
        String listKey = getMessageListKey(roomId);
        List<Object> objects = redisTemplate.opsForList().range(listKey, 0, -1);
        return convertToMessages(objects);
    }

    @Override
    public List<ChatRoom> findRoomsByUserId(String userId) {
        return findRoomsWithFilter(room -> room.getParticipants().contains(userId));
    }

    public List<ChatRoom> findRoomsCreatedBefore(LocalDateTime cutoffTime) {
        return findRoomsWithFilter(room -> {
            LocalDateTime createdAt = room.getCreatedAt();
            return createdAt == null || createdAt.isBefore(cutoffTime);
        });
    }

    private <T> void saveWithExpiration(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, CHAT_TTL, TimeUnit.DAYS);
    }

    private String getRoomKey(String roomId) {
        return ROOM_KEY_PREFIX + roomId;
    }

    private String getMessageKey(String roomId, String messageId) {
        return MESSAGE_KEY_PREFIX + roomId + ":" + messageId;
    }

    private String getMessageListKey(String roomId) {
        return MESSAGE_LIST_PREFIX + roomId;
    }

    private List<ChatMessage> convertToMessages(List<Object> objects) {
        if (objects == null) {
            return Collections.emptyList();
        }

        return objects.stream()
                .map(obj -> (ChatMessage) obj)
                .collect(Collectors.toList());
    }

    private List<ChatRoom> findRoomsWithFilter(Predicate<ChatRoom> filter) {
        Set<String> keys = redisTemplate.keys(ROOM_KEY_PREFIX + "*");

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        return keys.stream()
                .map(key -> (ChatRoom) redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .filter(filter)
                .collect(Collectors.toList());
    }
}