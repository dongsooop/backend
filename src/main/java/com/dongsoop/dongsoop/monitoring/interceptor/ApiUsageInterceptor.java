package com.dongsoop.dongsoop.monitoring.interceptor;

import com.dongsoop.dongsoop.monitoring.dto.ApiUsageEvent;
import com.dongsoop.dongsoop.monitoring.service.ApiUsageRecorder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@RequiredArgsConstructor
public class ApiUsageInterceptor implements HandlerInterceptor {

    static final String START_NANOS_ATTRIBUTE = ApiUsageInterceptor.class.getName() + ".start";
    static final String UNKNOWN_URI = "UNKNOWN";
    static final String ANONYMOUS_ACTOR = "anon";
    private static final String DEVICE_FID_HEADER = "X-Device-Fid";
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final ApiUsageRecorder recorder;

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
        return new ApiUsageEvent(
                ZonedDateTime.now(ZONE),
                request.getMethod(),
                uri,
                featureOf(uri),
                response.getStatus(),
                elapsedMillis(request),
                memberId,
                deviceId,
                actorOf(memberId, deviceId, request.getHeader(DEVICE_FID_HEADER))
        );
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
