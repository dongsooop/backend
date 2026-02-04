package com.dongsoop.dongsoop.jwt.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.exception.NotAccessTokenException;
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import com.dongsoop.dongsoop.jwt.exception.TokenRoleNotAvailableException;
import com.dongsoop.dongsoop.jwt.exception.TokenSignatureException;
import com.dongsoop.dongsoop.jwt.exception.TokenUnsupportedException;
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
@DisplayName("JwtFilter - 토큰 검증 테스트")
class JwtFilterTokenValidationTest {

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
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("유효한 액세스 토큰이면 인증 정보를 SecurityContext에 저장한다")
    void whenValidAccessToken_thenSetAuthentication() throws Exception {
        // given
        String token = "validAccessToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(token)).thenReturn(authentication);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(jwtUtil).getClaims(token);
        verify(jwtValidator).validate(claims);
        verify(jwtValidator).validateAccessToken(claims);
        verify(jwtUtil).getAuthenticationByToken(token);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(authentication);
    }

    @Test
    @DisplayName("만료된 토큰이면 예외를 처리하고 필터 체인을 중단한다")
    void whenExpiredToken_thenHandleException() throws Exception {
        // given
        String token = "expiredToken";
        TokenExpiredException exception = new TokenExpiredException(new RuntimeException());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenThrow(exception);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("잘못된 서명의 토큰이면 예외를 처리하고 필터 체인을 중단한다")
    void whenInvalidSignatureToken_thenHandleException() throws Exception {
        // given
        String token = "invalidSignatureToken";
        TokenSignatureException exception = new TokenSignatureException(new RuntimeException());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenThrow(exception);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("잘못된 형식의 토큰이면 예외를 처리하고 필터 체인을 중단한다")
    void whenMalformedToken_thenHandleException() throws Exception {
        // given
        String token = "malformedToken";
        TokenMalformedException exception = new TokenMalformedException(new RuntimeException());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenThrow(exception);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("지원하지 않는 토큰이면 예외를 처리하고 필터 체인을 중단한다")
    void whenUnsupportedToken_thenHandleException() throws Exception {
        // given
        String token = "unsupportedToken";
        TokenUnsupportedException exception = new TokenUnsupportedException(new RuntimeException());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenThrow(exception);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Refresh 토큰으로 접근하면 예외를 처리하고 필터 체인을 중단한다")
    void whenRefreshToken_thenHandleException() throws Exception {
        // given
        String token = "refreshToken";
        NotAccessTokenException exception = new NotAccessTokenException();
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        doNothing().when(jwtValidator).validate(claims);
        doThrow(exception).when(jwtValidator).validateAccessToken(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("Role이 없는 토큰이면 예외를 처리하고 필터 체인을 중단한다")
    void whenTokenWithoutRole_thenHandleException() throws Exception {
        // given
        String token = "tokenWithoutRole";
        TokenRoleNotAvailableException exception = new TokenRoleNotAvailableException();
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        doThrow(exception).when(jwtValidator).validate(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("토큰의 Subject가 숫자가 아니면 예외를 처리하고 필터 체인을 중단한다")
    void whenTokenWithInvalidSubject_thenHandleException() throws Exception {
        // given
        String token = "tokenWithInvalidSubject";
        TokenMalformedException exception = new TokenMalformedException();
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        doThrow(exception).when(jwtValidator).validate(claims);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
        verify(filterChain, never()).doFilter(any(), any());
    }
}
