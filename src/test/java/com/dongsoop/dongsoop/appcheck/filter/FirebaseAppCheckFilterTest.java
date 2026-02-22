package com.dongsoop.dongsoop.appcheck.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.appcheck.exception.UnknownFirebaseFetchJWKException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("FirebaseAppCheckFilter 테스트")
class FirebaseAppCheckFilterTest {

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
    private FirebaseAppCheckFilter firebaseAppCheckFilter;

    @BeforeEach
    void setUp() {
        firebaseAppCheckFilter = new FirebaseAppCheckFilter(firebaseAppCheck, exceptionResolver, new String[]{});
    }

    @Test
    @DisplayName("Firebase AppCheck 헤더가 있으면 검증을 시도한다")
    void whenFirebaseAppCheckHeaderExists_thenValidate() throws Exception {
        // given
        String deviceToken = "firebaseAppCheckToken";
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(deviceToken);

        doNothing().when(firebaseAppCheck).validate(deviceToken);

        // when
        firebaseAppCheckFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck, never()).updateCache();
        verify(firebaseAppCheck).validate(deviceToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Firebase AppCheck 검증 실패 시 캐시 업데이트 후 재검증 성공")
    void whenFirebaseAppCheckValidationFails_thenRetryAfterCacheUpdate() throws Exception {
        // given
        String deviceToken = "validTokenAfterUpdate";
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(deviceToken);

        doNothing().when(firebaseAppCheck).updateCache();
        doThrow(new UnknownFirebaseFetchJWKException())
                .doNothing()
                .when(firebaseAppCheck).validate(deviceToken);

        // when
        firebaseAppCheckFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck).updateCache();
        verify(firebaseAppCheck, org.mockito.Mockito.times(2)).validate(deviceToken);
        verify(filterChain).doFilter(request, response);
        verify(exceptionResolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Firebase AppCheck 검증 실패 후 재검증도 실패하면 예외 처리")
    void whenFirebaseAppCheckValidationFailsTwice_thenResolveException() throws Exception {
        // given
        String deviceToken = "invalidToken";
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(deviceToken);

        doNothing().when(firebaseAppCheck).updateCache();
        doThrow(new UnknownFirebaseFetchJWKException())
                .when(firebaseAppCheck).validate(deviceToken);

        // when
        firebaseAppCheckFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck).updateCache();
        verify(firebaseAppCheck, org.mockito.Mockito.times(2)).validate(deviceToken);
        verify(filterChain, never()).doFilter(request, response);
        verify(exceptionResolver).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Firebase AppCheck 헤더가 없으면 예외를 처리하고 필터 체인을 중단한다")
    void whenNoFirebaseAppCheckHeader_thenResolveExceptionAndStopFilterChain() throws Exception {
        // given
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(null);

        // when
        firebaseAppCheckFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck, never()).updateCache();
        verify(firebaseAppCheck, never()).validate(any());
        verify(filterChain, never()).doFilter(request, response);
        verify(exceptionResolver).resolveException(any(), any(), any(), any());
    }


    @Test
    @DisplayName("Firebase AppCheck updateCache 실패 시 예외를 처리하고 필터 체인을 중단한다")
    void whenFirebaseAppCheckUpdateCacheFails_thenResolveExceptionAndStopFilterChain() throws Exception {
        // given
        String deviceToken = "someToken";
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn(deviceToken);

        doThrow(new UnknownFirebaseFetchJWKException(new RuntimeException("Cache update failed")))
                .when(firebaseAppCheck).validate(deviceToken);
        doThrow(new UnknownFirebaseFetchJWKException(new RuntimeException("Cache update failed")))
                .when(firebaseAppCheck).updateCache();

        // when
        firebaseAppCheckFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck).validate(deviceToken);
        verify(firebaseAppCheck).updateCache();
        verify(filterChain, never()).doFilter(request, response);
        verify(exceptionResolver).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("빈 문자열 토큰인 경우 예외를 처리하고 필터 체인을 중단한다")
    void whenEmptyDeviceToken_thenResolveExceptionAndStopFilterChain() throws Exception {
        // given
        when(request.getHeader("X-Firebase-AppCheck")).thenReturn("");

        // when
        firebaseAppCheckFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(firebaseAppCheck, never()).updateCache();
        verify(firebaseAppCheck, never()).validate(any());
        verify(filterChain, never()).doFilter(request, response);
        verify(exceptionResolver).resolveException(any(), any(), any(), any());
    }

    @ParameterizedTest(name = "경로 ''{0}'' → shouldNotFilter: {1}")
    @CsvSource({
            "/ws/connect,    true",
            "/ws/room/123,   true",
            "/ws/,           true",
            "/member/login,  false",
            "/device,        false",
            "/ws-other,      false"
    })
    @DisplayName("shouldNotFilter: Ant 경로 패턴(/ws/**)에 대한 매칭 검증")
    void shouldNotFilter_withAntPattern(String requestUri, boolean expectedResult) throws Exception {
        // given
        FirebaseAppCheckFilter filterWithPaths = new FirebaseAppCheckFilter(
                firebaseAppCheck, exceptionResolver, new String[]{"/ws/**"});
        when(request.getRequestURI()).thenReturn(requestUri);

        // when
        boolean result = filterWithPaths.shouldNotFilter(request);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    @DisplayName("shouldNotFilter: ignorePaths가 비어있으면 모든 경로에 필터를 적용한다")
    void shouldNotFilter_whenIgnorePathsEmpty_thenAlwaysFilter() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/ws/connect");

        // when
        boolean result = firebaseAppCheckFilter.shouldNotFilter(request);

        // then
        assertThat(result).isFalse();
    }
}
