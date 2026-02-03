package com.dongsoop.dongsoop.jwt.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.exception.NotAccessTokenException;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter - 통합 시나리오 테스트")
class JwtFilterIntegrationTest {

    private final String[] ignorePaths = {"/api/public/**", "/oauth2/**", "/login"};
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private JwtValidator jwtValidator;
    @Mock
    private FirebaseAppCheck firebaseAppCheck;
    @Mock
    private HandlerExceptionResolver exceptionResolver;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private Claims claims;
    @Mock
    private Authentication authentication;
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil, jwtValidator, firebaseAppCheck, exceptionResolver, ignorePaths);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("완전한 성공 시나리오: Firebase AppCheck + 유효한 JWT 토큰")
    void completeSuccessScenario() throws Exception {
        // given
        String deviceToken = "validAppCheckToken";
        String jwtToken = "validJwtToken";
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(deviceToken);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);

        doNothing().when(firebaseAppCheck).updateCache();
        doNothing().when(firebaseAppCheck).validate(deviceToken);
        when(jwtUtil.getClaims(jwtToken)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(jwtToken)).thenReturn(authentication);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck).updateCache();
        verify(firebaseAppCheck).validate(deviceToken);
        verify(jwtUtil).getClaims(jwtToken);
        verify(jwtValidator).validate(claims);
        verify(jwtValidator).validateAccessToken(claims);
        verify(jwtUtil).getAuthenticationByToken(jwtToken);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    @DisplayName("공개 API 접근 시나리오: 필터를 거치지 않음")
    void publicApiAccessScenario() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/api/public/boards");

        // when
        boolean shouldNotFilter = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("인증 없이 보호된 API 접근 시나리오: 토큰 없이 필터 통과")
    void protectedApiWithoutAuthScenario() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(exceptionResolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("만료된 토큰으로 접근 시나리오: 예외 처리 후 필터 체인 중단")
    void expiredTokenAccessScenario() throws Exception {
        // given
        String expiredToken = "expiredJwtToken";
        TokenExpiredException exception = new TokenExpiredException(new RuntimeException());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(expiredToken)).thenThrow(exception);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Refresh 토큰으로 일반 API 접근 시나리오: 예외 처리")
    void refreshTokenAccessScenario() throws Exception {
        // given
        String refreshToken = "refreshJwtToken";
        NotAccessTokenException exception = new NotAccessTokenException();
        when(request.getHeader("Authorization")).thenReturn("Bearer " + refreshToken);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(refreshToken)).thenReturn(claims);
        doNothing().when(jwtValidator).validate(claims);
        doThrow(exception).when(jwtValidator).validateAccessToken(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("로그인 경로 접근 시나리오: 필터를 거치지 않음")
    void loginPathAccessScenario() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/login");

        // when
        boolean shouldNotFilter = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("OAuth2 경로 접근 시나리오: 필터를 거치지 않음")
    void oauth2PathAccessScenario() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/google");

        // when
        boolean shouldNotFilter = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(shouldNotFilter).isTrue();
    }

    @Test
    @DisplayName("동일한 필터 인스턴스로 연속된 요청 처리")
    void multipleRequestsWithSameFilterInstance() throws Exception {
        // 첫 번째 요청 - 유효한 토큰
        String validToken = "validToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + validToken);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(validToken)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(validToken)).thenReturn(authentication);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
        verify(filterChain).doFilter(request, response);

        // SecurityContext 초기화 (새로운 요청 시뮬레이션)
        SecurityContextHolder.clearContext();
        reset(filterChain, jwtUtil, jwtValidator);

        // 두 번째 요청 - 토큰 없음
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).getClaims(any());
    }

    @Test
    @DisplayName("Firebase AppCheck 실패해도 유효한 JWT로 인증 성공")
    void firebaseAppCheckFailsButJwtSucceeds() throws Exception {
        // given
        String deviceToken = "invalidAppCheckToken";
        String jwtToken = "validJwtToken";
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(deviceToken);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);

        doNothing().when(firebaseAppCheck).updateCache();
        doThrow(new RuntimeException("Invalid AppCheck token")).when(firebaseAppCheck).validate(deviceToken);
        when(jwtUtil.getClaims(jwtToken)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(jwtToken)).thenReturn(authentication);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck).validate(deviceToken);
        verify(jwtUtil).getClaims(jwtToken);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    @DisplayName("잘못된 Authorization 헤더 형식이지만 필터 체인은 계속 진행")
    void invalidAuthorizationHeaderFormatButContinues() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token123");
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).getClaims(any());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
