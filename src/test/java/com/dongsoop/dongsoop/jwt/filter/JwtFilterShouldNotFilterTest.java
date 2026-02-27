package com.dongsoop.dongsoop.jwt.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter - 필터 제외 경로 테스트")
class JwtFilterShouldNotFilterTest {

    private final String[] ignorePaths = {
            "/api/public/**",
            "/oauth2/**",
            "/login",
            "/api/auth/refresh"
    };
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
    private DeviceBlacklistService deviceBlacklistService;
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtFilter(jwtUtil, jwtValidator, deviceBlacklistService, exceptionResolver, ignorePaths);
    }

    @Test
    @DisplayName("필터 제외 경로(/api/public/*)는 필터를 적용하지 않는다")
    void whenPublicApiPath_thenShouldNotFilter() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/api/public/test");

        // when
        boolean result = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("필터 제외 경로(/oauth2/*)는 필터를 적용하지 않는다")
    void whenOAuth2Path_thenShouldNotFilter() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/oauth2/authorization/google");

        // when
        boolean result = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("필터 제외 경로(/login)는 필터를 적용하지 않는다")
    void whenLoginPath_thenShouldNotFilter() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/login");

        // when
        boolean result = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("필터 제외 경로(/api/auth/refresh)는 필터를 적용하지 않는다")
    void whenRefreshPath_thenShouldNotFilter() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/api/auth/refresh");

        // when
        boolean result = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("일반 API 경로는 필터를 적용한다")
    void whenNormalApiPath_thenShouldFilter() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/api/user/profile");

        // when
        boolean result = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("보호된 경로는 필터를 적용한다")
    void whenProtectedPath_thenShouldFilter() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/api/admin/users");

        // when
        boolean result = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("와일드카드 패턴이 하위 경로에도 적용된다")
    void whenWildcardPattern_thenMatchesSubPaths() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/api/public/boards/123/comments");

        // when
        boolean result = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("경로가 정확히 일치하지 않으면 필터를 적용한다")
    void whenPathDoesNotMatch_thenShouldFilter() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/public/test"); // /api/public이 아님

        // when
        boolean result = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("빈 경로는 필터를 적용한다")
    void whenEmptyPath_thenShouldFilter() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("");

        // when
        boolean result = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("루트 경로는 필터를 적용한다")
    void whenRootPath_thenShouldFilter() throws Exception {
        // given
        when(request.getRequestURI()).thenReturn("/");

        // when
        boolean result = jwtFilter.shouldNotFilter(request);

        // then
        assertThat(result).isFalse();
    }
}
