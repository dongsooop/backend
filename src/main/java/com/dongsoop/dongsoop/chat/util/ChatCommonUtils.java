package com.dongsoop.dongsoop.chat.util;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.redis.core.RedisTemplate;

public final class ChatCommonUtils {
    private static final String CONTACT_MAPPING_KEY_PREFIX = "contact_mapping";

    private ChatCommonUtils() {
    }

    public static void validatePositiveUserId(Long userId) {
        if (userId == null) {
            throw new UnauthorizedChatAccessException();
        }
        if (userId < 0) {
            throw new UnauthorizedChatAccessException();
        }
    }

    public static String generateMessageId() {
        return UUID.randomUUID().toString();
    }

    public static LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    public static String createSystemMessageContent(Long userId) {
        return userId.toString();
    }

    public static void enrichMessageId(ChatMessage message) {
        String currentMessageId = message.getMessageId();

        if (currentMessageId == null || currentMessageId.isEmpty()) {
            message.setMessageId(generateMessageId());
        }
    }

    public static void enrichMessageTimestamp(ChatMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(getCurrentTime());
        }
    }

    public static void enrichMessageType(ChatMessage message) {
        if (message.getType() == null) {
            message.setType(MessageType.CHAT);
        }
    }

    public static void enrichMessage(ChatMessage message) {
        enrichMessageId(message);
        enrichMessageTimestamp(message);
        enrichMessageType(message);
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String createContactMappingKey(Long userId, Long targetUserId, Object boardType, Long boardId) {
        Long user1 = Math.min(userId, targetUserId);
        Long user2 = Math.max(userId, targetUserId);
        String typeStr = boardType.toString(); // RecruitmentType.PROJECT 또는 MarketplaceType.SELL
        return String.format("%s:%d:%d:%s:%d", CONTACT_MAPPING_KEY_PREFIX, user1, user2, typeStr, boardId);
    }

    public static String findExistingContactRoomId(RedisTemplate<String, Object> redisTemplate,
                                                   Long userId, Long targetUserId,
                                                   Object boardType, Long boardId) {
        String mappingKey = createContactMappingKey(userId, targetUserId, boardType, boardId);
        Object result = redisTemplate.opsForValue().get(mappingKey);

        if (result instanceof String) {
            return (String) result;
        }
        return null;
    }

    public static void saveContactRoomMapping(RedisTemplate<String, Object> redisTemplate,
                                              Long userId, Long targetUserId,
                                              Object boardType, Long boardId, String roomId) {
        String mappingKey = createContactMappingKey(userId, targetUserId, boardType, boardId);
        redisTemplate.opsForValue().set(mappingKey, roomId);
    }
}