package com.dongsoop.dongsoop.jwt.service;

import com.dongsoop.dongsoop.jwt.exception.BlacklistedTokenException;
import java.time.Duration;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 디바이스 기반 JWT 블랙리스트를 Redis로 관리한다.
 *
 * <p>로그아웃(본인/강제) 요청 시 {@code device:blacklist:{deviceId} → 블랙리스트 등록 시각(epoch millis)} 형태로 저장한다.
 * 이후 요청에서 JWT의 {@code did} claim이 블랙리스트에 존재하고,
 * JWT 발급일({@code iat})이 등록 시각 이하이면 해당 토큰을 거부한다.
 *
 * <p>항목의 TTL은 리프레시 토큰 만료 시간과 동일하다.
 * 리프레시 토큰이 만료되면 블랙리스트 등록 전에 발급된 구 토큰도 모두 자연 만료되므로,
 * 해당 시점에 Redis 항목도 자동 삭제된다.
 *
 * <p>토큰에 {@code did} claim이 없는 경우(서비스 배포 전 발급된 기존 토큰)는
 * 블랙리스트 검사를 건너뛰어 하위 호환성을 유지한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DeviceBlacklistService {

    private static final String KEY_PREFIX = "device:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.expired-time.refresh-token}")
    private long refreshTokenExpiredTime;

    /**
     * 디바이스를 블랙리스트에 등록한다.
     *
     * <p>현재 시각을 등록 시각으로 저장하며 TTL은 리프레시 토큰 만료 시간으로 설정한다.
     * 해당 시각 이전에 발급된 토큰은 이후 요청에서 모두 거부된다.
     *
     * @param deviceId 블랙리스트에 등록할 디바이스 ID
     */
    public void blacklist(Long deviceId) {
        String key = KEY_PREFIX + deviceId;
        String blacklistedAt = String.valueOf(System.currentTimeMillis());
        Duration ttl = Duration.ofMillis(refreshTokenExpiredTime);

        stringRedisTemplate.opsForValue().set(key, blacklistedAt, ttl);
        log.debug("Device {} added to blacklist", deviceId);
    }

    /**
     * 토큰이 블랙리스트에 해당하는지 검사하고, 해당하면 예외를 던진다.
     *
     * <p>{@code issuedAt}이 {@code null}인 경우(발급일 claim이 없는 기존 토큰)는
     * 검사를 건너뛰어 하위 호환성을 유지한다.
     *
     * @param deviceId JWT의 did claim
     * @param issuedAt JWT의 발급일 (iat claim), null 허용
     * @throws BlacklistedTokenException 토큰이 블랙리스트 등록 시각 이하에 발급된 경우
     */
    public void validateNotBlacklisted(Long deviceId, Date issuedAt) {
        if (issuedAt == null) {
            return;
        }

        String key = KEY_PREFIX + deviceId;
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null) {
            return;
        }

        long blacklistedAt = Long.parseLong(value);
        if (issuedAt.getTime() <= blacklistedAt) {
            throw new BlacklistedTokenException();
        }
    }
}
