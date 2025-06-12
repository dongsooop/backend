package com.dongsoop.dongsoop.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadStatusService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String USER_LAST_READ_PREFIX = "user:lastread:";
    private static final String USER_JOIN_TIME_PREFIX = "user:jointime:";
    private static final long TTL_DAYS = 30;

    public void initializeUserReadStatus(Long userId, String roomId, LocalDateTime joinTime) {
        updateLastReadTimestamp(userId, roomId, joinTime);
        saveUserJoinTime(userId, roomId, joinTime);

        log.debug("사용자 {} 채팅방 {} 읽음 상태 초기화: {}", userId, roomId, joinTime);
    }

    public void updateLastReadTimestamp(Long userId, String roomId, LocalDateTime timestamp) {
        String key = buildUserLastReadKey(userId, roomId);
        String timestampStr = timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        redisTemplate.opsForValue().set(key, timestampStr, TTL_DAYS, TimeUnit.DAYS);

        log.debug("사용자 {} 채팅방 {} 마지막 읽음 시간 업데이트: {}", userId, roomId, timestamp);
    }

    public LocalDateTime getLastReadTimestamp(Long userId, String roomId) {
        String key = buildUserLastReadKey(userId, roomId);
        String timestampStr = redisTemplate.opsForValue().get(key);

        return Optional.ofNullable(timestampStr)
                .map(str -> LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .orElseGet(() -> getUserJoinTime(userId, roomId));
    }

    public LocalDateTime getUserJoinTime(Long userId, String roomId) {
        String key = buildUserJoinTimeKey(userId, roomId);
        String timestampStr = redisTemplate.opsForValue().get(key);

        return Optional.ofNullable(timestampStr)
                .map(str -> LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .orElse(null);
    }

    private void saveUserJoinTime(Long userId, String roomId, LocalDateTime joinTime) {
        String joinTimeKey = buildUserJoinTimeKey(userId, roomId);
        String joinTimeStr = joinTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        redisTemplate.opsForValue().set(joinTimeKey, joinTimeStr, TTL_DAYS, TimeUnit.DAYS);
    }

    private String buildUserLastReadKey(Long userId, String roomId) {
        return USER_LAST_READ_PREFIX + userId + ":" + roomId;
    }

    private String buildUserJoinTimeKey(Long userId, String roomId) {
        return USER_JOIN_TIME_PREFIX + userId + ":" + roomId;
    }
}