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
import com.dongsoop.dongsoop.jwt.exception.TokenExpiredException;
import com.dongsoop.dongsoop.jwt.exception.TokenMalformedException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
@DisplayName("JwtFilter - 예외 처리 테스트")
class JwtFilterExceptionHandlingTest {

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
    @DisplayName("JWT 예외 발생 시 ExceptionResolver를 호출하고 필터 체인을 중단한다")
    void whenJwtExceptionOccurs_thenCallExceptionResolverAndStopFilterChain() throws Exception {
        // given
        String token = "invalidToken";
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
    @DisplayName("JWT 예외 발생 시 인증 정보를 설정하지 않는다")
    void whenJwtExceptionOccurs_thenNoAuthenticationSet() throws Exception {
        // given
        String token = "invalidToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenThrow(new TokenMalformedException(new RuntimeException()));

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("FilterChain에서 예외 발생 시 ExceptionResolver를 호출한다")
    void whenFilterChainThrowsException_thenCallExceptionResolver() throws Exception {
        // given
        String token = "validToken";
        ServletException exception = new ServletException("Filter chain error");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(token)).thenReturn(authentication);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);
        doThrow(exception).when(filterChain).doFilter(request, response);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
    }

    @Test
    @DisplayName("FilterChain에서 IOException 발생 시 ExceptionResolver를 호출한다")
    void whenFilterChainThrowsIOException_thenCallExceptionResolver() throws Exception {
        // given
        String token = "validToken";
        IOException exception = new IOException("IO error");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(token)).thenReturn(authentication);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);
        doThrow(exception).when(filterChain).doFilter(request, response);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
    }

    @Test
    @DisplayName("FilterChain에서 RuntimeException 발생 시 ExceptionResolver를 호출한다")
    void whenFilterChainThrowsRuntimeException_thenCallExceptionResolver() throws Exception {
        // given
        String token = "validToken";
        RuntimeException exception = new RuntimeException("Runtime error");
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenReturn(claims);
        when(jwtUtil.getAuthenticationByToken(token)).thenReturn(authentication);
        doNothing().when(jwtValidator).validate(claims);
        doNothing().when(jwtValidator).validateAccessToken(claims);
        doThrow(exception).when(filterChain).doFilter(request, response);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception));
    }

    @Test
    @DisplayName("JWT 검증 중 예외가 발생하면 ExceptionResolver가 정확한 예외를 전달받는다")
    void whenJwtValidationFails_thenExceptionResolverReceivesCorrectException() throws Exception {
        // given
        String token = "expiredToken";
        TokenExpiredException expectedException = new TokenExpiredException(new RuntimeException("Token expired"));
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenThrow(expectedException);

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(expectedException));
    }

    @Test
    @DisplayName("예외 발생 후에도 SecurityContext는 초기 상태를 유지한다")
    void whenExceptionOccurs_thenSecurityContextRemainsClean() throws Exception {
        // given
        String token = "invalidToken";
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token)).thenThrow(new TokenMalformedException(new RuntimeException()));

        // when
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("여러 예외가 연속으로 발생해도 각각 올바르게 처리된다")
    void whenMultipleExceptionsOccur_thenEachIsHandledCorrectly() throws Exception {
        // given - 첫 번째 요청에서 예외 발생
        String token1 = "token1";
        TokenExpiredException exception1 = new TokenExpiredException(new RuntimeException());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token1);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token1)).thenThrow(exception1);

        // when - 첫 번째 요청 처리
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception1));

        // given - 두 번째 요청에서 다른 예외 발생
        reset(exceptionResolver, jwtUtil);
        String token2 = "token2";
        TokenMalformedException exception2 = new TokenMalformedException(new RuntimeException());
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token2);
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);
        when(jwtUtil.getClaims(token2)).thenThrow(exception2);

        // when - 두 번째 요청 처리
        jwtFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), eq(exception2));
    }
}
