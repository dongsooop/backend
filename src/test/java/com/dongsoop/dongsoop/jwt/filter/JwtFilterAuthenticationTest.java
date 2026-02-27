package com.dongsoop.dongsoop.jwt.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.exception.BlacklistedTokenException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter - 인증 설정 테스트")
class JwtFilterAuthenticationTest {

    private final String[] ignorePaths = {"/api/public/**", "/oauth2/**"};
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private JwtValidator jwtValidator;

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
    @Mock
    private DeviceBlacklistService deviceBlacklistService;
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil, jwtValidator, deviceBlacklistService, exceptionResolver, ignorePaths);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 토큰으로 인증 정보를 SecurityContext에 설정한다")
    void whenValidToken_thenSetAuthenticationInSecurityContext() throws Exception {
        // given
        String token = "validToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        when(jwtUtil.getClaims(token)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(token)).thenReturn(authentication);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull()
                .isEqualTo(authentication);
    }

    @Test
    @DisplayName("인증 정보 설정 후 필터 체인이 계속 진행된다")
    void whenAuthenticationSet_thenFilterChainContinues() throws Exception {
        // given
        String token = "validToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        when(jwtUtil.getClaims(token)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(token)).thenReturn(authentication);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰이 없으면 인증 정보를 설정하지 않는다")
    void whenNoToken_thenNoAuthenticationSet() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);


        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).getAuthenticationByToken(any());
    }

    @Test
    @DisplayName("토큰 검증 실패 시 인증 정보를 설정하지 않는다")
    void whenTokenValidationFails_thenNoAuthenticationSet() throws Exception {
        // given
        String token = "invalidToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        when(jwtUtil.getClaims(token)).thenReturn(claims);
        doThrow(new TokenMalformedException()).when(jwtValidator).validate(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).getAuthenticationByToken(any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("블랙리스트에 등록된 디바이스 토큰은 인증 정보를 설정하지 않는다")
    void whenBlacklistedDevice_thenNoAuthenticationSet() throws Exception {
        // given
        String token = "blacklistedToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);
        when(claims.get(JwtUtil.DEVICE_ID_CLAIM, Long.class)).thenReturn(42L);
        doThrow(new BlacklistedTokenException()).when(deviceBlacklistService)
                .validateNotBlacklisted(any(), any());

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).getAuthenticationByToken(any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("각 요청마다 SecurityContext가 독립적으로 설정된다")
    void whenMultipleRequests_thenSecurityContextIsIndependent() throws Exception {
        // given
        String token1 = "token1";
        String token2 = "token2";
        Authentication auth1 = mock(Authentication.class);
        Authentication auth2 = mock(Authentication.class);



        // 첫 번째 요청
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token1);
        when(jwtUtil.getClaims(token1)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(token1)).thenReturn(auth1);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);

        // when - 첫 번째 요청 처리
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(auth1);

        // given - 두 번째 요청
        SecurityContextHolder.clearContext();
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token2);
        when(jwtUtil.getClaims(token2)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(token2)).thenReturn(auth2);

        // when - 두 번째 요청 처리
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(auth2);
    }
}
