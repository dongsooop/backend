package com.dongsoop.dongsoop.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadStatusService {

    private static final String USER_LAST_READ_PREFIX = "user:lastread:";
    private static final String USER_JOIN_TIME_PREFIX = "user:jointime:";
    private static final long TTL_DAYS = 30;
    private final RedisTemplate<String, String> redisTemplate;

    public void initializeUserReadStatus(Long userId, String roomId, LocalDateTime joinTime) {
        updateLastReadTimestamp(userId, roomId, joinTime);
        saveUserJoinTime(userId, roomId, joinTime);

        log.debug("사용자 {} 채팅방 {} 읽음 상태 초기화: {}", userId, roomId, joinTime);
    }

    public void updateLastReadTimestamp(Long userId, String roomId, LocalDateTime timestamp) {
        String key = buildUserLastReadKey(userId, roomId);
        String timestampStr = formatTimestamp(timestamp);

        redisTemplate.opsForValue().set(key, timestampStr, TTL_DAYS, TimeUnit.DAYS);

        log.debug("사용자 {} 채팅방 {} 마지막 읽음 시간 업데이트: {}", userId, roomId, timestamp);
    }

    public LocalDateTime getLastReadTimestamp(Long userId, String roomId) {
        String key = buildUserLastReadKey(userId, roomId);
        String timestampStr = redisTemplate.opsForValue().get(key);

        return parseTimestampOrGetJoinTime(timestampStr, userId, roomId);
    }

    public Map<String, LocalDateTime> getLastReadTimestampsBatch(Long userId, List<String> roomIds) {
        List<String> lastReadKeys = roomIds.stream()
                .map(roomId -> buildUserLastReadKey(userId, roomId))
                .toList();

        List<String> lastReadValues = redisTemplate.opsForValue().multiGet(lastReadKeys);

        List<String> joinTimeKeys = roomIds.stream()
                .map(roomId -> buildUserJoinTimeKey(userId, roomId))
                .toList();

        List<String> joinTimeValues = redisTemplate.opsForValue().multiGet(joinTimeKeys);

        Map<String, LocalDateTime> result = new HashMap<>();
        for (int index = 0; index < roomIds.size(); index++) {
            String roomId = roomIds.get(index);
            String timestampStr = (lastReadValues != null && index < lastReadValues.size()) ? lastReadValues.get(index) : null;

            // 읽음 시간이 있으면 바로 저장 후 다음으로
            if (timestampStr != null) {
                result.put(roomId, parseTimestamp(timestampStr));
                continue;
            }

            // 읽음 시간 없으면 joinTime으로 대체
            String joinTimeStr = (joinTimeValues != null && index < joinTimeValues.size()) ? joinTimeValues.get(index) : null;
            result.put(roomId, parseTimestamp(joinTimeStr));
        }
        return result;
    }

    public Map<Long, LocalDateTime> getLastReadTimestampsBatchForUsers(List<Long> userIds, String roomId) {
        List<String> lastReadKeys = userIds.stream()
                .map(userId -> buildUserLastReadKey(userId, roomId))
                .toList();

        List<String> lastReadValues = redisTemplate.opsForValue().multiGet(lastReadKeys);

        List<String> joinTimeKeys = userIds.stream()
                .map(userId -> buildUserJoinTimeKey(userId, roomId))
                .toList();

        List<String> joinTimeValues = redisTemplate.opsForValue().multiGet(joinTimeKeys);

        Map<Long, LocalDateTime> result = new HashMap<>();
        for (int index = 0; index < userIds.size(); index++) {
            Long userId = userIds.get(index);
            String timestampStr = (lastReadValues != null && index < lastReadValues.size()) ? lastReadValues.get(index) : null;

            // 읽음 시간이 있으면 바로 저장 후 다음으로
            if (timestampStr != null) {
                result.put(userId, parseTimestamp(timestampStr));
                continue;
            }

            // 읽음 시간 없으면 joinTime으로 대체
            String joinTimeStr = (joinTimeValues != null && index < joinTimeValues.size()) ? joinTimeValues.get(index) : null;
            result.put(userId, parseTimestamp(joinTimeStr));
        }
        return result;
    }

    public LocalDateTime getUserJoinTime(Long userId, String roomId) {
        String key = buildUserJoinTimeKey(userId, roomId);
        String timestampStr = redisTemplate.opsForValue().get(key);

        return parseTimestamp(timestampStr);
    }

    private void saveUserJoinTime(Long userId, String roomId, LocalDateTime joinTime) {
        String joinTimeKey = buildUserJoinTimeKey(userId, roomId);
        String joinTimeStr = formatTimestamp(joinTime);
        redisTemplate.opsForValue().set(joinTimeKey, joinTimeStr, TTL_DAYS, TimeUnit.DAYS);
    }

    private LocalDateTime parseTimestampOrGetJoinTime(String timestampStr, Long userId, String roomId) {
        if (timestampStr == null) {
            return getUserJoinTime(userId, roomId);
        }
        return parseTimestamp(timestampStr);
    }

    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null) {
            return null;
        }
        return LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String buildUserLastReadKey(Long userId, String roomId) {
        return USER_LAST_READ_PREFIX + userId + ":" + roomId;
    }

    private String buildUserJoinTimeKey(Long userId, String roomId) {
        return USER_JOIN_TIME_PREFIX + userId + ":" + roomId;
    }
}
