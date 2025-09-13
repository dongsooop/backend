package com.dongsoop.dongsoop.chat.util;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatCommonUtils {
    private static final String CONTACT_MAPPING_KEY_PREFIX = "contact_mapping";
    private static final String RECRUITMENT_END_AT_PREFIX = "recruitment:room:";
    private static final String END_AT_SUFFIX = ":end_at";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private final RedisTemplate<String, String> redisTemplate;



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

    public static String createContactMappingKey(Long userId, Long targetUserId, RecruitmentType boardType, Long boardId) {
        Long user1 = Math.min(userId, targetUserId);
        Long user2 = Math.max(userId, targetUserId);
        String typeStr = boardType.name();
        return String.format("%s:%d:%d:%s:%d", CONTACT_MAPPING_KEY_PREFIX, user1, user2, typeStr, boardId);
    }

    public static String findExistingContactRoomId(RedisTemplate<String, Object> redisTemplate,
                                                   Long userId, Long targetUserId,
                                                   RecruitmentType boardType, Long boardId) {
        String mappingKey = createContactMappingKey(userId, targetUserId, boardType, boardId);
        Object result = redisTemplate.opsForValue().get(mappingKey);

        if (result instanceof String) {
            return (String) result;
        }
        return null;
    }

    public static void saveContactRoomMapping(RedisTemplate<String, Object> redisTemplate,
                                              Long userId, Long targetUserId,
                                              RecruitmentType boardType, Long boardId, String roomId) {
        String mappingKey = createContactMappingKey(userId, targetUserId, boardType, boardId);

        // 기존: boardInfo → roomId 매핑
        redisTemplate.opsForValue().set(mappingKey, roomId);

        // 추가: roomId → boardInfo 역매핑
        String reverseMappingKey = "room_to_contact:" + roomId;
        String boardInfo = String.format("%d:%d:%s:%d",
                Math.min(userId, targetUserId),
                Math.max(userId, targetUserId),
                boardType.name(),
                boardId);
        redisTemplate.opsForValue().set(reverseMappingKey, boardInfo);
    }

    public static void deleteContactRoomMapping(RedisTemplate<String, Object> redisTemplate, String roomId) {
        // 역매핑에서 boardInfo 조회
        String reverseMappingKey = "room_to_contact:" + roomId;
        String boardInfo = (String) redisTemplate.opsForValue().get(reverseMappingKey);

        if (boardInfo != null) {
            // 원래 매핑 키 생성 후 삭제
            String originalMappingKey = CONTACT_MAPPING_KEY_PREFIX + ":" + boardInfo;
            redisTemplate.delete(originalMappingKey);
            redisTemplate.delete(reverseMappingKey);
        }
    }

    public void saveRecruitmentEndAt(String roomId, LocalDateTime endAt) {
        String key = buildEndAtKey(roomId);
        String endAtStr = endAt.format(FORMATTER);
        
        Duration ttl = Duration.between(LocalDateTime.now(), endAt.plusDays(1));
        redisTemplate.opsForValue().set(key, endAtStr, ttl);
        
        log.info("모집 채팅방 종료일 저장: roomId={}, endAt={}", roomId, endAt);
    }

    public Optional<LocalDateTime> getRecruitmentEndAt(String roomId) {
        String key = buildEndAtKey(roomId);
        String endAtStr = redisTemplate.opsForValue().get(key);
        
        if (endAtStr == null) {
            return Optional.empty();
        }
        
        return Optional.of(LocalDateTime.parse(endAtStr, FORMATTER));
    }

    public void removeRecruitmentInfo(String roomId) {
        String key = buildEndAtKey(roomId);
        redisTemplate.delete(key);
        log.info("모집 채팅방 정보 삭제: roomId={}", roomId);
    }

    private String buildEndAtKey(String roomId) {
        return RECRUITMENT_END_AT_PREFIX + roomId + END_AT_SUFFIX;
    }
}