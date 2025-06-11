package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

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
        String key = buildRoomKey(room.getRoomId());
        saveWithTTL(key, room);
        return room;
    }

    @Override
    public Optional<ChatRoom> findRoomById(String roomId) {
        String key = buildRoomKey(roomId);
        return Optional.ofNullable((ChatRoom) redisTemplate.opsForValue().get(key));
    }

    @Override
    public Optional<ChatRoom> findRoomByParticipants(Long user1, Long user2) {
        return findRoomsWithFilter(room -> {
            Set<Long> participants = room.getParticipants();
            return !room.isGroupChat() &&
                    participants.contains(user1) &&
                    participants.contains(user2);
        }).stream().findFirst();
    }

    @Override
    public void saveMessage(ChatMessage message) {
        String messageKey = buildMessageKey(message.getRoomId(), message.getMessageId());
        String listKey = buildMessageListKey(message.getRoomId());

        saveWithTTL(messageKey, message);
        addMessageToList(listKey, message);
    }

    @Override
    public List<ChatMessage> findMessagesByRoomId(String roomId) {
        String listKey = buildMessageListKey(roomId);
        List<Object> objects = redisTemplate.opsForList().range(listKey, 0, -1);

        return Optional.ofNullable(objects)
                .orElse(Collections.emptyList())
                .stream()
                .map(obj -> (ChatMessage) obj)
                .toList();
    }

    @Override
    public List<ChatRoom> findRoomsByUserId(Long userId) {
        return findRoomsWithFilter(room -> room.getParticipants().contains(userId));
    }

    public List<ChatRoom> findRoomsWithLastActivityBefore(LocalDateTime cutoffTime) {
        return findRoomsWithFilter(room -> {
            LocalDateTime lastActivityAt = room.getLastActivityAt();
            return lastActivityAt == null || lastActivityAt.isBefore(cutoffTime);
        });
    }

    public void deleteRoom(String roomId) {
        String roomKey = buildRoomKey(roomId);
        String messageListKey = buildMessageListKey(roomId);

        redisTemplate.delete(roomKey);
        redisTemplate.delete(messageListKey);

        Set<String> messageKeys = redisTemplate.keys(MESSAGE_KEY_PREFIX + roomId + ":*");
        if (messageKeys != null && !messageKeys.isEmpty()) {
            redisTemplate.delete(messageKeys);
        }
    }

    private String buildRoomKey(String roomId) {
        return ROOM_KEY_PREFIX + roomId;
    }

    private String buildMessageKey(String roomId, String messageId) {
        return MESSAGE_KEY_PREFIX + roomId + ":" + messageId;
    }

    private String buildMessageListKey(String roomId) {
        return MESSAGE_LIST_PREFIX + roomId;
    }

    private void saveWithTTL(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, CHAT_TTL, TimeUnit.DAYS);
    }

    private void addMessageToList(String listKey, ChatMessage message) {
        redisTemplate.opsForList().rightPush(listKey, message);
        redisTemplate.expire(listKey, CHAT_TTL, TimeUnit.DAYS);
    }

    private List<ChatRoom> findRoomsWithFilter(Predicate<ChatRoom> filter) {
        Set<String> keys = redisTemplate.keys(ROOM_KEY_PREFIX + "*");

        return Optional.ofNullable(keys)
                .orElse(Collections.emptySet())
                .stream()
                .map(key -> (ChatRoom) redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .filter(filter)
                .toList();
    }
}