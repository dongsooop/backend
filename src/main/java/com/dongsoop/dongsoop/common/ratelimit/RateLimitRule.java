package com.dongsoop.dongsoop.common.ratelimit;

import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpMethod;

/**
 * Rate Limit 규칙. 특정 HTTP 메서드 + 경로 패턴 묶음에 대해 시간창(window) 동안 허용 횟수(limit)를 정의한다.
 *
 * @param name        Redis 키 prefix 및 로깅에 사용하는 식별자
 * @param method      적용할 HTTP 메서드
 * @param pathPatterns Ant 스타일 경로 패턴 목록
 * @param limit       window 동안 허용하는 최대 요청 수
 * @param window      카운터가 유지되는 시간창
 */
public record RateLimitRule(
        String name,
        HttpMethod method,
        List<String> pathPatterns,
        int limit,
        Duration window
) {
}
