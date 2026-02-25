package com.dongsoop.dongsoop.chat.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatCommonUtils {
    private static final String RECRUITMENT_END_AT_PREFIX = "recruitment:room:";
    private static final String END_AT_SUFFIX = ":end_at";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final RedisTemplate<String, String> redisTemplate;

    public void saveRecruitmentEndAt(String roomId, LocalDateTime endAt) {
        String key = buildEndAtKey(roomId);
        String endAtStr = endAt.format(FORMATTER);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryTime = endAt.plusDays(1);

        long secondsBetween = Duration.between(now, expiryTime).toSeconds();
        long ttlInSeconds = Math.max(1L, secondsBetween);

        Duration ttl = Duration.ofSeconds(ttlInSeconds);

        if (secondsBetween < 1) {
            log.warn("saveRecruitmentEndAt called with past endAt: roomId={}, endAt={}. Setting minimal TTL.", roomId,
                    endAt);
        }

        redisTemplate.opsForValue().set(key, endAtStr, ttl);
        log.info("Group ChatRoom EndDay Save: roomId={}, endAt={}, ttlSeconds={}", roomId, endAt, ttl.toSeconds());
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
