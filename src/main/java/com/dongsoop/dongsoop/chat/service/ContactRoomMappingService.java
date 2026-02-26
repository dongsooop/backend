package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.search.entity.BoardType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactRoomMappingService {
    private static final String CONTACT_MAPPING_KEY_PREFIX = "contact";
    private static final String MARKETPLACE = "MARKETPLACE";

    private final RedisTemplate<String, Object> redisTemplate;

    public String findExistingContactRoomId(Long userId, Long targetUserId,
                                            BoardType boardType, Long boardId) {
        String mappingKey = createContactMappingKey(userId, targetUserId, boardType, boardId);
        Object result = redisTemplate.opsForValue().get(mappingKey);

        if (result instanceof String) {
            return (String) result;
        }
        return null;
    }

    public void saveContactRoomMapping(Long userId, Long targetUserId,
                                       BoardType boardType, Long boardId, String roomId) {
        String mappingKey = createContactMappingKey(userId, targetUserId, boardType, boardId);

        redisTemplate.opsForValue().set(mappingKey, roomId);

        String reverseMappingKey = "room_to_contact:" + roomId;
        String boardInfo = String.format("%d:%d:%s:%d",
                Math.min(userId, targetUserId),
                Math.max(userId, targetUserId),
                boardType.getCode(),
                boardId);
        redisTemplate.opsForValue().set(reverseMappingKey, boardInfo);
    }

    public void deleteContactRoomMapping(String roomId) {
        String reverseMappingKey = "room_to_contact:" + roomId;
        String boardInfo = (String) redisTemplate.opsForValue().get(reverseMappingKey);

        if (boardInfo != null) {
            String originalMappingKey = CONTACT_MAPPING_KEY_PREFIX + ":" + boardInfo;
            redisTemplate.delete(originalMappingKey);
            redisTemplate.delete(reverseMappingKey);
        }
    }

    private String createContactMappingKey(Long userId, Long targetUserId, BoardType boardType, Long boardId) {
        String boardTypeName = boardType != null ? boardType.getCode() : MARKETPLACE;

        Long minUserId = Math.min(userId, targetUserId);
        Long maxUserId = Math.max(userId, targetUserId);

        return String.format("contact:%d:%d:%s:%d", minUserId, maxUserId, boardTypeName, boardId);
    }
}
