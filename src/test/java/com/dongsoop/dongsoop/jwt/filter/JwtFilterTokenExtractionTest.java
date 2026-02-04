package com.dongsoop.dongsoop.jwt.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter - 토큰 추출 테스트")
class JwtFilterTokenExtractionTest {

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
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil, jwtValidator, firebaseAppCheck, exceptionResolver, ignorePaths);
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 TokenNotFoundException이 발생하지 않고 필터가 계속 진행된다")
    void whenNoAuthorizationHeader_thenContinueFilterChain() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).getClaims(any());
        verify(exceptionResolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Authorization 헤더가 비어있으면 토큰 추출 없이 필터가 계속 진행된다")
    void whenEmptyAuthorizationHeader_thenContinueFilterChain() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("");
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).getClaims(any());
    }

    @Test
    @DisplayName("Bearer 접두사가 없으면 토큰 추출 없이 필터가 계속 진행된다")
    void whenNoBearerPrefix_thenContinueFilterChain() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("InvalidToken");
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).getClaims(any());
    }

    @Test
    @DisplayName("Bearer 접두사만 있고 토큰이 없으면 토큰 추출 없이 필터가 계속 진행된다")
    void whenOnlyBearerPrefix_thenContinueFilterChain() throws Exception {
        // given
        when(request.getHeader("Authorization")).thenReturn("Bearer ");
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);
        verify(jwtUtil, never()).getClaims(any());
    }

    @Test
    @DisplayName("Bearer 다음에 공백만 있는 경우 토큰으로 인식되어 검증이 시도된다")
    void whenBearerWithOnlySpaces_thenTokenValidationAttempted() throws Exception {
        // given
        String token = "   "; // 공백들이 토큰으로 추출됨
        when(request.getHeader("Authorization")).thenReturn("Bearer    ");
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenThrow(new com.dongsoop.dongsoop.jwt.exception.TokenMalformedException());

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).getClaims(token);
        verify(exceptionResolver).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("올바른 형식의 Authorization 헤더가 있으면 토큰을 추출하고 검증한다")
    void whenValidAuthorizationHeader_thenExtractAndValidateToken() throws Exception {
        // given
        String token = "validToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).getClaims(token);
        verify(jwtValidator).validate(claims);
        verify(jwtValidator).validateAccessToken(claims);
    }
}
