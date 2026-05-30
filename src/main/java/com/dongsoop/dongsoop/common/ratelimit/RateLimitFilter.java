package com.dongsoop.dongsoop.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 남용 위험이 높은 엔드포인트(로그인/이메일/검색)에 대해 클라이언트 IP 기준 요청 횟수를 제한한다.
 *
 * <p>Redis INCR + EXPIRE 고정 시간창 방식이라 멀티 인스턴스 배포에서도 카운터가 공유된다.
 * Redis 장애 시에는 가용성을 위해 요청을 통과시킨다(fail-open).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "rate-limit:";

    // INCR 후 카운터가 처음 생성된 경우(==1)에만 TTL을 설정하여 고정 시간창을 원자적으로 적용한다.
    // INCR/EXPIRE 분리 시 둘 사이에 장애가 나면 TTL 없는 영구 키가 남는 문제를 방지한다.
    private static final RedisScript<Long> INCREMENT_WITH_EXPIRE = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]) "
                    + "if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
                    + "return count",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitRules rateLimitRules;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        RateLimitRule rule = matchRule(request);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        if (isLimitExceeded(rule, clientIp)) {
            writeTooManyRequests(response, rule);
            return;
        }

        chain.doFilter(request, response);
    }

    private RateLimitRule matchRule(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        for (RateLimitRule rule : rateLimitRules.rules()) {
            if (!rule.method().matches(method)) {
                continue;
            }
            boolean pathMatched = rule.pathPatterns().stream()
                    .anyMatch(pattern -> pathMatcher.match(pattern, path));
            if (pathMatched) {
                return rule;
            }
        }

        return null;
    }

    private boolean isLimitExceeded(RateLimitRule rule, String clientIp) {
        String key = KEY_PREFIX + rule.name() + ":" + clientIp;

        try {
            Long count = redisTemplate.execute(
                    INCREMENT_WITH_EXPIRE,
                    List.of(key),
                    String.valueOf(rule.window().toMillis()));
            if (count == null) {
                return false;
            }
            return count > rule.limit();
        } catch (Exception e) {
            // Redis 장애 시 가용성을 우선하여 통과시킨다.
            log.warn("Rate limit check failed for key {}, allowing request", key, e);
            return false;
        }
    }

    private void writeTooManyRequests(HttpServletResponse response, RateLimitRule rule) throws IOException {
        long retryAfterSeconds = Math.max(1, rule.window().getSeconds());

        if (response.isCommitted()) {
            return;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"title\":\"Too Many Requests\",\"status\":429,"
                        + "\"detail\":\"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
    }

    private String resolveClientIp(HttpServletRequest request) {
        // raw X-Forwarded-For 직접 파싱은 스푸핑에 취약하므로 사용하지 않는다.
        // 운영은 server.forward-headers-strategy=framework 로 신뢰된 프록시의 XFF만 반영되어
        // getRemoteAddr()가 실제 클라이언트 IP를 반환한다. 로컬은 직접 연결 IP를 사용한다.
        return request.getRemoteAddr();
    }
}
