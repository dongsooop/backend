package com.dongsoop.dongsoop.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.monitoring.dto.ApiUsageEvent;
import com.dongsoop.dongsoop.monitoring.interceptor.ApiUsageInterceptor;
import com.dongsoop.dongsoop.monitoring.service.ApiUsageRecorder;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

class ApiUsageInterceptorTest {

    private final ApiUsageRecorder recorder = mock(ApiUsageRecorder.class);
    private final ApiUsageInterceptor interceptor = new ApiUsageInterceptor(recorder);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("핸들러 패턴·상태·회원·기기가 문서에 담기고 actor는 회원이 우선")
    void recordsAuthenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/project-board/17");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/project-board/{boardId}");
        request.addHeader("X-Device-Fid", "fid-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(12L, null, List.of());
        auth.setDetails(34L);
        SecurityContextHolder.getContext().setAuthentication(auth);

        interceptor.preHandle(request, response, new Object());
        interceptor.afterCompletion(request, response, new Object(), null);

        verify(recorder).offer(any(ApiUsageEvent.class));
        ApiUsageEvent event = interceptor.toEvent(request, response);
        assertThat(event.uri()).isEqualTo("/project-board/{boardId}");
        assertThat(event.feature()).isEqualTo("project-board");
        assertThat(event.method()).isEqualTo("GET");
        assertThat(event.status()).isEqualTo(200);
        assertThat(event.memberId()).isEqualTo(12L);
        assertThat(event.deviceId()).isEqualTo(34L);
        assertThat(event.actor()).isEqualTo("m:12");
        assertThat(event.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("비회원은 기기 ID, 그것도 없으면 fid 헤더, 셋 다 없으면 anon")
    void actorFallsBackInOrder() {
        assertThat(ApiUsageInterceptor.actorOf(null, 34L, "fid")).isEqualTo("d:34");
        assertThat(ApiUsageInterceptor.actorOf(null, null, "fid")).isEqualTo("f:fid");
        assertThat(ApiUsageInterceptor.actorOf(null, null, " ")).isEqualTo("anon");
    }

    @Test
    @DisplayName("매핑 안 된 요청은 UNKNOWN, 버전 접두어는 건너뛰고 기능을 잡는다")
    void resolvesUriAndFeature() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/nothing");
        assertThat(ApiUsageInterceptor.resolveUri(request)).isEqualTo("UNKNOWN");
        assertThat(ApiUsageInterceptor.featureOf("UNKNOWN")).isEqualTo("unknown");
        assertThat(ApiUsageInterceptor.featureOf("/v2/notice/keywords")).isEqualTo("notice");
        assertThat(ApiUsageInterceptor.featureOf("/eclass")).isEqualTo("eclass");
        assertThat(ApiUsageInterceptor.featureOf("/")).isEqualTo("root");
    }

    @Test
    @DisplayName("자동 호출 경로는 기능 system 으로, 제외 회원은 actor admin 으로 기록된다")
    void classifiesSystemUriAndAdmin() {
        ApiUsageInterceptor configured = new ApiUsageInterceptor(recorder,
                List.of("/token/**", "/device", "/reports/sanction-status"), java.util.Set.of(502L));
        MockHttpServletResponse response = new MockHttpServletResponse();

        MockHttpServletRequest reissue = new MockHttpServletRequest("POST", "/token/reissue");
        reissue.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/token/reissue");
        assertThat(configured.toEvent(reissue, response).feature()).isEqualTo("system");

        MockHttpServletRequest sanction = new MockHttpServletRequest("GET", "/reports/sanction-status");
        sanction.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/reports/sanction-status");
        assertThat(configured.toEvent(sanction, response).feature()).isEqualTo("system");

        MockHttpServletRequest report = new MockHttpServletRequest("POST", "/reports");
        report.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/reports");
        UsernamePasswordAuthenticationToken admin = new UsernamePasswordAuthenticationToken(502L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(admin);
        ApiUsageEvent event = configured.toEvent(report, response);
        assertThat(event.feature()).isEqualTo("reports");
        assertThat(event.actor()).isEqualTo("admin");
        assertThat(event.memberId()).isEqualTo(502L);
    }

    @Test
    @DisplayName("기록 중 예외가 나도 요청 처리로 번지지 않는다")
    void swallowsRecorderFailure() {
        when(recorder.offer(any())).thenThrow(new IllegalStateException("boom"));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/home");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.afterCompletion(request, response, new Object(), null);
    }
}
