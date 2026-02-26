package com.dongsoop.dongsoop.jwt.service;

import com.dongsoop.dongsoop.jwt.exception.BlacklistedTokenException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 디바이스 기반 JWT 블랙리스트를 Redis로 관리한다.
 *
 * <p>로그아웃(본인/강제) 요청 시 {@code device:blacklist:{deviceId}} 키에
 * 블랙리스트 등록 시각(epoch millis)을 저장한다.
 * 이후 요청에서 JWT의 {@code deviceId} claim이 블랙리스트에 존재하고,
 * JWT 발급일({@code iat})이 블랙리스트 등록 시각보다 이전이면 해당 토큰을 거부한다.
 *
 * <p>토큰에 {@code deviceId}가 없는 경우(서비스 배포 전 발급된 기존 토큰)는
 * 블랙리스트 검사를 건너뛰어 하위 호환성을 유지한다.
 */
@Service
@RequiredArgsConstructor
public class DeviceBlacklistService {

    private static final String KEY_PREFIX = "device:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.expired-time.refresh-token}")
    private long refreshTokenExpiredTime;

    /**
     * 디바이스를 블랙리스트에 등록한다.
     *
     * <p>현재 시각을 등록 시각으로 저장하며, TTL은 리프레시 토큰 유효 시간으로 설정한다.
     * TTL 이후에는 모든 구 토큰이 자연 만료되므로 블랙리스트 항목이 더 이상 필요하지 않다.
     *
     * @param deviceId 블랙리스트에 등록할 디바이스 ID
     */
    public void blacklist(Long deviceId) {
        String key = KEY_PREFIX + deviceId;
        String blacklistedAt = String.valueOf(System.currentTimeMillis());
        stringRedisTemplate.opsForValue().set(key, blacklistedAt, refreshTokenExpiredTime, TimeUnit.MILLISECONDS);
    }

    /**
     * 토큰이 블랙리스트에 해당하는지 검사하고, 해당하면 예외를 던진다.
     *
     * <p>{@code issuedAt}이 {@code null}인 경우(발급일 claim이 없는 기존 토큰)는
     * 검사를 건너뛰어 하위 호환성을 유지한다.
     *
     * @param deviceId JWT의 deviceId claim
     * @param issuedAt JWT의 발급일 (iat claim), null 허용
     * @throws BlacklistedTokenException 토큰이 블랙리스트 등록 이전에 발급된 경우
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

        long blacklistedAtMillis = Long.parseLong(value);
        if (issuedAt.getTime() < blacklistedAtMillis) {
            throw new BlacklistedTokenException();
        }
    }
}
