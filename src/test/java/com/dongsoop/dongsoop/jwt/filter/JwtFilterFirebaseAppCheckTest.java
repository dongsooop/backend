package com.dongsoop.dongsoop.jwt.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.appcheck.exception.UnknownFirebaseFetchJWKException;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
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
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter - Firebase AppCheck 테스트")
class JwtFilterFirebaseAppCheckTest {

    private final String[] ignorePaths = {"/api/public/**", "/oauth2/**"};
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
    }

    @Test
    @DisplayName("Firebase AppCheck 헤더가 있으면 검증을 시도한다")
    void whenFirebaseAppCheckHeaderExists_thenValidate() throws Exception {
        // given
        String deviceToken = "firebaseAppCheckToken";
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(deviceToken);
        when(request.getHeader("Authorization")).thenReturn(null);
        doNothing().when(firebaseAppCheck).updateCache();
        doNothing().when(firebaseAppCheck).validate(deviceToken);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck).updateCache();
        verify(firebaseAppCheck).validate(deviceToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Firebase AppCheck 검증 실패 시 로그만 남기고 필터 체인을 계속 진행한다")
    void whenFirebaseAppCheckValidationFails_thenContinueFilterChain() throws Exception {
        // given
        String deviceToken = "invalidAppCheckToken";
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(deviceToken);
        when(request.getHeader("Authorization")).thenReturn(null);
        doNothing().when(firebaseAppCheck).updateCache();
        doThrow(new UnknownFirebaseFetchJWKException()).when(firebaseAppCheck).validate(deviceToken);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck).validate(deviceToken);
        verify(filterChain).doFilter(request, response);
        verify(exceptionResolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Firebase AppCheck 헤더가 없으면 검증을 시도하고 실패하지만 필터 체인은 계속 진행한다")
    void whenNoFirebaseAppCheckHeader_thenContinueFilterChain() throws Exception {
        // given
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);
        doNothing().when(firebaseAppCheck).updateCache();

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck).updateCache();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Firebase AppCheck와 JWT 토큰이 모두 유효하면 인증 정보를 설정하고 필터 체인을 진행한다")
    void whenBothFirebaseAppCheckAndJwtValid_thenSetAuthenticationAndContinue() throws Exception {
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
        verify(firebaseAppCheck).validate(deviceToken);
        verify(jwtUtil).getClaims(jwtToken);
        verify(jwtValidator).validate(claims);
        verify(jwtValidator).validateAccessToken(claims);
        verify(jwtUtil).getAuthenticationByToken(jwtToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Firebase AppCheck 검증 실패해도 JWT 토큰 검증은 진행된다")
    void whenFirebaseAppCheckFailsButJwtValid_thenStillValidateJwt() throws Exception {
        // given
        String deviceToken = "invalidAppCheckToken";
        String jwtToken = "validJwtToken";
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(deviceToken);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + jwtToken);
        doNothing().when(firebaseAppCheck).updateCache();
        doThrow(new UnknownFirebaseFetchJWKException(new RuntimeException("Invalid token"))).when(firebaseAppCheck)
                .validate(deviceToken);
        when(jwtUtil.getClaims(jwtToken)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(jwtToken)).thenReturn(authentication);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck).validate(deviceToken);
        verify(jwtUtil).getClaims(jwtToken);
        verify(jwtValidator).validate(claims);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Firebase AppCheck updateCache 실패 시에도 필터 체인은 계속 진행한다")
    void whenFirebaseAppCheckUpdateCacheFails_thenContinueFilterChain() throws Exception {
        // given
        lenient().when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        lenient().when(request.getHeader("Authorization")).thenReturn(null);
        doThrow(new RuntimeException("Cache update failed")).when(firebaseAppCheck).updateCache();

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(exceptionResolver, never()).resolveException(any(), any(), any(), any());
    }
}
