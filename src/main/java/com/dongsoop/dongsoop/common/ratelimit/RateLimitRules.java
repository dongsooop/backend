package com.dongsoop.dongsoop.common.ratelimit;

import java.util.List;

/**
 * Rate Limit 규칙 묶음 홀더.
 *
 * <p>{@code List<RateLimitRule>}를 직접 빈으로 주입하면 Spring이 "RateLimitRule 타입 빈 수집"으로
 * 해석해 빈 리스트가 주입되는 함정이 있어, 전용 홀더 타입으로 감싸 모호성을 제거한다.
 */
public record RateLimitRules(List<RateLimitRule> rules) {
}
