package com.dongsoop.dongsoop.jwt.service;

import com.dongsoop.dongsoop.jwt.exception.BlacklistedTokenException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 디바이스 기반 JWT 블랙리스트를 인메모리 Map으로 관리한다.
 *
 * <p>로그아웃(본인/강제) 요청 시 {@code deviceId → 블랙리스트 등록 시각(epoch millis)} 형태로 저장한다.
 * 이후 요청에서 JWT의 {@code did} claim이 블랙리스트에 존재하고,
 * JWT 발급일({@code iat})이 등록 시각보다 이전이면 해당 토큰을 거부한다.
 *
 * <p>항목의 유효 기간은 리프레시 토큰 만료 시간과 동일하며,
 * 조회 시점에 만료된 항목은 자동으로 제거된다(lazy expiration).
 *
 * <p>토큰에 {@code did} claim이 없는 경우(서비스 배포 전 발급된 기존 토큰)는
 * 블랙리스트 검사를 건너뛰어 하위 호환성을 유지한다.
 */
@Service
@Slf4j
public class DeviceBlacklistService {

    // deviceId → 블랙리스트 등록 시각 (epoch millis)
    private final ConcurrentHashMap<Long, Long> blacklist = new ConcurrentHashMap<>();

    @Value("${jwt.expired-time.refresh-token}")
    private long refreshTokenExpiredTime;

    /**
     * 디바이스를 블랙리스트에 등록한다.
     *
     * <p>현재 시각을 등록 시각으로 저장한다.
     * 해당 시각 이전에 발급된 토큰은 이후 요청에서 모두 거부된다.
     *
     * @param deviceId 블랙리스트에 등록할 디바이스 ID
     */
    public void blacklist(Long deviceId) {
        blacklist.put(deviceId, System.currentTimeMillis());
        log.debug("Device {} added to blacklist", deviceId);
    }

    /**
     * 토큰이 블랙리스트에 해당하는지 검사하고, 해당하면 예외를 던진다.
     *
     * <p>{@code issuedAt}이 {@code null}인 경우(발급일 claim이 없는 기존 토큰)는
     * 검사를 건너뛰어 하위 호환성을 유지한다.
     * 블랙리스트 항목이 리프레시 토큰 유효 기간을 초과하면 만료된 것으로 간주하고
     * 항목을 제거한 뒤 통과시킨다.
     *
     * @param deviceId JWT의 did claim
     * @param issuedAt JWT의 발급일 (iat claim), null 허용
     * @throws BlacklistedTokenException 토큰이 블랙리스트 등록 이전에 발급된 경우
     */
    public void validateNotBlacklisted(Long deviceId, Date issuedAt) {
        if (issuedAt == null) {
            return;
        }

        Long blacklistedAt = blacklist.get(deviceId);
        if (blacklistedAt == null) {
            return;
        }

        // 블랙리스트 항목이 refreshTokenExpiredTime을 초과하면 모든 구 토큰이 자연 만료됨 → 항목 제거 후 통과
        if (System.currentTimeMillis() - blacklistedAt > refreshTokenExpiredTime) {
            blacklist.remove(deviceId);
            log.debug("Expired blacklist entry removed for device {}", deviceId);
            return;
        }

        if (issuedAt.getTime() <= blacklistedAt) {
            throw new BlacklistedTokenException();
        }
    }
}
