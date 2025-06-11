package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisChatRepository implements ChatRepository {
    private static final long CHAT_TTL = 30; // 30일 TTL
    private static final String ROOM_KEY_PREFIX = "chat:room:";
    private static final String MESSAGE_KEY_PREFIX = "chat:message:";
    private static final String MESSAGE_LIST_PREFIX = "chat:messages:";
    private static final String USER_ROOM_INDEX_PREFIX = "user:rooms:";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisChatRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ChatRoom saveRoom(ChatRoom room) {
        String key = buildRoomKey(room.getRoomId());
        saveWithTTL(key, room);
        indexRoomForUsers(room);
        return room;
    }

    @Override
    public Optional<ChatRoom> findRoomById(String roomId) {
        String key = buildRoomKey(roomId);
        return Optional.ofNullable((ChatRoom) redisTemplate.opsForValue().get(key));
    }

    @Override
    public Optional<ChatRoom> findRoomByParticipants(Long user1, Long user2) {
        return findDirectRoomByParticipants(user1, user2);
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
        return findRoomsByUserIdDirect(userId);
    }

    public List<ChatRoom> findRoomsWithLastActivityBefore(LocalDateTime cutoffTime) {
        Set<String> keys = redisTemplate.keys(ROOM_KEY_PREFIX + "*");

        return Optional.ofNullable(keys)
                .orElse(Collections.emptySet())
                .stream()
                .map(key -> (ChatRoom) redisTemplate.opsForValue().get(key))
                .filter(Objects::nonNull)
                .filter(room -> isLastActivityBefore(room, cutoffTime))
                .toList();
    }

    public void deleteRoom(String roomId) {
        ChatRoom room = findRoomById(roomId).orElse(null);

        Optional.ofNullable(room)
                .ifPresent(r -> {
                    removeRoomIndexes(r);
                    deleteRoomData(roomId);
                });
    }

    private Optional<ChatRoom> findDirectRoomByParticipants(Long user1, Long user2) {
        Set<String> user1Rooms = getUserRoomIds(user1);
        Set<String> user2Rooms = getUserRoomIds(user2);

        return user1Rooms.stream()
                .filter(user2Rooms::contains)
                .map(this::findRoomById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(room -> isOneToOneRoom(room, user1, user2))
                .findFirst();
    }

    private List<ChatRoom> findRoomsByUserIdDirect(Long userId) {
        Set<String> roomIds = getUserRoomIds(userId);

        return roomIds.stream()
                .map(this::findRoomById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Set<String> getUserRoomIds(Long userId) {
        String userIndexKey = buildUserRoomIndexKey(userId);
        Set<Object> roomIds = redisTemplate.opsForSet().members(userIndexKey);

        return Optional.ofNullable(roomIds)
                .orElse(Collections.emptySet())
                .stream()
                .map(Object::toString)
                .collect(HashSet::new, HashSet::add, HashSet::addAll);
    }

    private boolean isOneToOneRoom(ChatRoom room, Long user1, Long user2) {
        Set<Long> participants = room.getParticipants();
        return !room.isGroupChat() &&
                participants.contains(user1) &&
                participants.contains(user2) &&
                participants.size() == 2;
    }

    private boolean isLastActivityBefore(ChatRoom room, LocalDateTime cutoffTime) {
        LocalDateTime lastActivityAt = room.getLastActivityAt();
        return lastActivityAt == null || lastActivityAt.isBefore(cutoffTime);
    }

    private void indexRoomForUsers(ChatRoom room) {
        room.getParticipants().forEach(userId -> {
            String userIndexKey = buildUserRoomIndexKey(userId);
            redisTemplate.opsForSet().add(userIndexKey, room.getRoomId());
            redisTemplate.expire(userIndexKey, CHAT_TTL, TimeUnit.DAYS);
        });
    }

    private void removeRoomIndexes(ChatRoom room) {
        room.getParticipants().forEach(userId -> {
            String userIndexKey = buildUserRoomIndexKey(userId);
            redisTemplate.opsForSet().remove(userIndexKey, room.getRoomId());
        });
    }

    private void deleteRoomData(String roomId) {
        String roomKey = buildRoomKey(roomId);
        String messageListKey = buildMessageListKey(roomId);

        redisTemplate.delete(roomKey);
        redisTemplate.delete(messageListKey);

        deleteRoomMessages(roomId);
    }

    private void deleteRoomMessages(String roomId) {
        Set<String> messageKeys = redisTemplate.keys(MESSAGE_KEY_PREFIX + roomId + ":*");
        Optional.ofNullable(messageKeys)
                .filter(keys -> !keys.isEmpty())
                .ifPresent(redisTemplate::delete);
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

    private String buildUserRoomIndexKey(Long userId) {
        return USER_ROOM_INDEX_PREFIX + userId;
    }

    private void saveWithTTL(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, CHAT_TTL, TimeUnit.DAYS);
    }

    private void addMessageToList(String listKey, ChatMessage message) {
        redisTemplate.opsForList().rightPush(listKey, message);
        redisTemplate.expire(listKey, CHAT_TTL, TimeUnit.DAYS);
    }
}