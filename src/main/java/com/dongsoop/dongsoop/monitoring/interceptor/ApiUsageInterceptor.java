package com.dongsoop.dongsoop.monitoring.interceptor;

import com.dongsoop.dongsoop.monitoring.dto.ApiUsageEvent;
import com.dongsoop.dongsoop.monitoring.service.ApiUsageRecorder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
public class ApiUsageInterceptor implements HandlerInterceptor {

    static final String START_NANOS_ATTRIBUTE = ApiUsageInterceptor.class.getName() + ".start";
    static final String UNKNOWN_URI = "UNKNOWN";
    static final String ANONYMOUS_ACTOR = "anon";
    /** 관리자 계정처럼 사용자 수에서 빼야 하는 호출 */
    public static final String ADMIN_ACTOR = "admin";
    /** 앱 시작 시 자동으로 나가는 호출(토큰 갱신, 기기 등록, 제재 확인 등). 기능 순위에서 뺀다 */
    public static final String SYSTEM_FEATURE = "system";
    private static final String DEVICE_FID_HEADER = "X-Device-Fid";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiUsageRecorder recorder;
    private final List<String> systemUriPatterns;
    private final Set<Long> excludedMemberIds;

    public ApiUsageInterceptor(ApiUsageRecorder recorder) {
        this(recorder, List.of(), Set.of());
    }

    public ApiUsageInterceptor(ApiUsageRecorder recorder, List<String> systemUriPatterns, Set<Long> excludedMemberIds) {
        this.recorder = recorder;
        this.systemUriPatterns = systemUriPatterns;
        this.excludedMemberIds = excludedMemberIds;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        request.setAttribute(START_NANOS_ATTRIBUTE, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        try {
            recorder.offer(toEvent(request, response));
        } catch (Exception e) {
            log.debug("Failed to record api usage: {}", e.getMessage());
        }
    }

    public ApiUsageEvent toEvent(HttpServletRequest request, HttpServletResponse response) {
        String uri = resolveUri(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long memberId = auth != null && auth.getPrincipal() instanceof Long id ? id : null;
        Long deviceId = auth != null && auth.getDetails() instanceof Long id ? id : null;
        String actor = memberId != null && excludedMemberIds.contains(memberId)
                ? ADMIN_ACTOR
                : actorOf(memberId, deviceId, request.getHeader(DEVICE_FID_HEADER));
        return new ApiUsageEvent(
                ZonedDateTime.now(ZONE),
                request.getMethod(),
                uri,
                isSystemUri(uri) ? SYSTEM_FEATURE : featureOf(uri),
                response.getStatus(),
                elapsedMillis(request),
                memberId,
                deviceId,
                actor
        );
    }

    boolean isSystemUri(String uri) {
        return systemUriPatterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, uri));
    }

    public static String resolveUri(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern instanceof String s && StringUtils.hasText(s) ? s : UNKNOWN_URI;
    }

    public static String featureOf(String uri) {
        if (UNKNOWN_URI.equals(uri)) {
            return "unknown";
        }
        String trimmed = uri.startsWith("/") ? uri.substring(1) : uri;
        String first = firstSegment(trimmed);
        // /v2/notice/... 처럼 버전 접두어가 붙은 경로는 그 다음 세그먼트가 기능이다
        if (first.matches("v\\d+")) {
            first = firstSegment(trimmed.substring(first.length() + 1));
        }
        return first.isEmpty() ? "root" : first;
    }

    private static String firstSegment(String path) {
        int slash = path.indexOf('/');
        return slash < 0 ? path : path.substring(0, slash);
    }

    public static String actorOf(Long memberId, Long deviceId, String fid) {
        if (memberId != null) {
            return "m:" + memberId;
        }
        if (deviceId != null) {
            return "d:" + deviceId;
        }
        if (StringUtils.hasText(fid)) {
            return "f:" + fid;
        }
        return ANONYMOUS_ACTOR;
    }

    private static long elapsedMillis(HttpServletRequest request) {
        Object start = request.getAttribute(START_NANOS_ATTRIBUTE);
        if (start instanceof Long nanos) {
            return (System.nanoTime() - nanos) / 1_000_000;
        }
        return 0;
    }
}
