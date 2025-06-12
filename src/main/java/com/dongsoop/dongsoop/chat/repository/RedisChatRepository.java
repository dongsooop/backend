package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisChatRepository implements ChatRepository {
    private static final long CHAT_TTL = 30;
    private static final String ROOM_KEY_PREFIX = "chat:room:";
    private static final String MESSAGE_KEY_PREFIX = "chat:message:";
    private static final String MESSAGE_ZSET_PREFIX = "chat:messages:sorted:";
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
        saveMessageToIndividualKey(message);
        addMessageToSortedSet(message);
    }

    @Override
    public List<ChatMessage> findMessagesByRoomId(String roomId) {
        String zsetKey = buildMessageZSetKey(roomId);
        Set<Object> messageIds = redisTemplate.opsForZSet().range(zsetKey, 0, -1);

        return loadMessagesFromIds(roomId, messageIds);
    }

    public List<ChatMessage> findMessagesByRoomIdAfterTime(String roomId, LocalDateTime afterTime) {
        String zsetKey = buildMessageZSetKey(roomId);
        double afterTimestamp = convertToTimestamp(afterTime);

        Set<Object> messageIds = redisTemplate.opsForZSet()
                .rangeByScore(zsetKey, afterTimestamp, Double.MAX_VALUE);

        return loadMessagesFromIds(roomId, messageIds);
    }

    public List<ChatMessage> findMessagesByRoomIdAfterId(String roomId, String lastMessageId) {
        String zsetKey = buildMessageZSetKey(roomId);

        Long lastIndex = findMessageRankInSortedSet(zsetKey, lastMessageId);

        // 메시지 ID를 찾을 수 없는 경우 명시적 처리
        validateMessageIdExists(lastIndex, lastMessageId, roomId);

        return retrieveMessagesAfterIndex(roomId, zsetKey, lastIndex);
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

    private void saveMessageToIndividualKey(ChatMessage message) {
        String messageKey = buildMessageKey(message.getRoomId(), message.getMessageId());
        saveWithTTL(messageKey, message);
    }

    private void addMessageToSortedSet(ChatMessage message) {
        String zsetKey = buildMessageZSetKey(message.getRoomId());
        double timestamp = convertToTimestamp(message.getTimestamp());

        redisTemplate.opsForZSet().add(zsetKey, message.getMessageId(), timestamp);
        redisTemplate.expire(zsetKey, CHAT_TTL, TimeUnit.DAYS);
    }

    private List<ChatMessage> loadMessagesFromIds(String roomId, Set<Object> messageIds) {
        return Optional.ofNullable(messageIds)
                .orElse(Collections.emptySet())
                .stream()
                .map(messageIdObj -> loadSingleMessage(roomId, messageIdObj.toString()))
                .filter(Objects::nonNull)
                .toList();
    }

    private ChatMessage loadSingleMessage(String roomId, String messageId) {
        String messageKey = buildMessageKey(roomId, messageId);
        return (ChatMessage) redisTemplate.opsForValue().get(messageKey);
    }

    private Long findMessageRankInSortedSet(String zsetKey, String messageId) {
        return redisTemplate.opsForZSet().rank(zsetKey, messageId);
    }

    private void validateMessageIdExists(Long lastIndex, String lastMessageId, String roomId) {
        Optional.ofNullable(lastIndex)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Message ID '" + lastMessageId + "' not found in room '" + roomId + "'"));
    }

    private List<ChatMessage> retrieveMessagesAfterIndex(String roomId, String zsetKey, Long lastIndex) {
        return Optional.ofNullable(lastIndex)
                .map(index -> retrieveMessagesFromRange(roomId, zsetKey, index + 1))
                .orElse(Collections.emptyList());
    }

    private List<ChatMessage> retrieveMessagesFromRange(String roomId, String zsetKey, long startIndex) {
        Set<Object> messageIds = redisTemplate.opsForZSet().range(zsetKey, startIndex, -1);
        return loadMessagesFromIds(roomId, messageIds);
    }

    private double convertToTimestamp(LocalDateTime dateTime) {
        return Optional.ofNullable(dateTime)
                .map(dt -> dt.toInstant(ZoneOffset.UTC).toEpochMilli())
                .map(Long::doubleValue)
                .orElse(0.0);
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
        room.getParticipants().forEach(userId -> indexRoomForSingleUser(room.getRoomId(), userId));
    }

    private void indexRoomForSingleUser(String roomId, Long userId) {
        String userIndexKey = buildUserRoomIndexKey(userId);
        redisTemplate.opsForSet().add(userIndexKey, roomId);
        redisTemplate.expire(userIndexKey, CHAT_TTL, TimeUnit.DAYS);
    }

    private void removeRoomIndexes(ChatRoom room) {
        room.getParticipants().forEach(userId -> removeRoomIndexForSingleUser(room.getRoomId(), userId));
    }

    private void removeRoomIndexForSingleUser(String roomId, Long userId) {
        String userIndexKey = buildUserRoomIndexKey(userId);
        redisTemplate.opsForSet().remove(userIndexKey, roomId);
    }

    private void deleteRoomData(String roomId) {
        deleteRoomKey(roomId);
        deleteMessageZSet(roomId);
        deleteRoomMessages(roomId);
    }

    private void deleteRoomKey(String roomId) {
        String roomKey = buildRoomKey(roomId);
        redisTemplate.delete(roomKey);
    }

    private void deleteMessageZSet(String roomId) {
        String messageZSetKey = buildMessageZSetKey(roomId);
        redisTemplate.delete(messageZSetKey);
    }

    private void deleteRoomMessages(String roomId) {
        Set<String> messageKeys = redisTemplate.keys(MESSAGE_KEY_PREFIX + roomId + ":*");
        deleteMessageKeysIfExists(messageKeys);
    }

    private void deleteMessageKeysIfExists(Set<String> messageKeys) {
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

    private String buildMessageZSetKey(String roomId) {
        return MESSAGE_ZSET_PREFIX + roomId;
    }

    private String buildUserRoomIndexKey(Long userId) {
        return USER_ROOM_INDEX_PREFIX + userId;
    }

    private void saveWithTTL(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        redisTemplate.expire(key, CHAT_TTL, TimeUnit.DAYS);
    }
}