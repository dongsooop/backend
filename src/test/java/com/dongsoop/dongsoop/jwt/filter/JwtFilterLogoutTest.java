package com.dongsoop.dongsoop.jwt.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.exception.BlacklistedTokenException;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter - /logout 요청 처리 테스트")
class JwtFilterLogoutTest {

    // application.yml의 authentication.filter.ignore.paths 실제 값
    private final String[] ignorePaths = {
            "/error",
            "/member/login",
            "/member/signup",
            "/mail-verify/**",
            "/notice/**",
            "/device",
            "/oauth2/authorization"
    };

    private static final Long DEVICE_ID = 42L;

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private JwtValidator jwtValidator;
    @Mock
    private DeviceBlacklistService deviceBlacklistService;
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

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil, jwtValidator, deviceBlacklistService, exceptionResolver, ignorePaths);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("/logout 경로는 JwtFilter 제외 대상이 아니다")
    void logout_path_is_not_excluded_from_jwt_filter() {
        when(request.getRequestURI()).thenReturn("/logout");

        assertThat(jwtFilter.shouldNotFilter(request)).isFalse();
    }

    @Test
    @DisplayName("강제 로그아웃된 디바이스의 토큰으로 /logout 요청 시 필터 체인이 중단된다")
    void when_blacklisted_device_requests_logout_then_filter_chain_is_stopped() throws Exception {
        String token = "force-logged-out-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);
        when(claims.get(JwtUtil.DEVICE_ID_CLAIM, Long.class)).thenReturn(DEVICE_ID);
        doThrow(new BlacklistedTokenException())
                .when(deviceBlacklistService).validateNotBlacklisted(any(), any());

        jwtFilter.doFilterInternal(request, response, filterChain);

        // LogoutFilter(JwtLogoutHandler)에 도달하지 않는다
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("강제 로그아웃된 디바이스의 토큰으로 /logout 요청 시 SecurityContext에 인증 정보가 설정되지 않는다")
    void when_blacklisted_device_requests_logout_then_authentication_is_not_set() throws Exception {
        String token = "force-logged-out-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);
        when(claims.get(JwtUtil.DEVICE_ID_CLAIM, Long.class)).thenReturn(DEVICE_ID);
        doThrow(new BlacklistedTokenException())
                .when(deviceBlacklistService).validateNotBlacklisted(any(), any());

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("블랙리스트 TTL 만료 후에는 액세스 토큰도 만료되어 /logout 요청이 차단된다")
    void when_blacklist_ttl_expired_then_access_token_is_also_expired() throws Exception {
        // 블랙리스트 TTL(10분) > 액세스 토큰 TTL(5분)이므로
        // TTL 만료 시점에는 액세스 토큰도 이미 만료 상태
        String expiredToken = "expired-after-blacklist-ttl-token";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + expiredToken);
        doThrow(new TokenExpiredException())
                .when(jwtUtil).getClaims(expiredToken);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
    }
}
