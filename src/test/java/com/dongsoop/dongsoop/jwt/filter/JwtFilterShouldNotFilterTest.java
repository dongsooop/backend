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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest(name = "{0}: 제외={1}")
    @CsvSource(value = {
            "/api/public/test, true",
            "/oauth2/authorization/google, true",
            "/login, true",
            "/api/auth/refresh, true",
            "/api/user/profile, false",
            "/api/admin/users, false",
            "/api/public/boards/123/comments, true",
            "/public/test, false",
            "'', false",
            "/, false"
    })
    @DisplayName("기존 제외 패턴의 정확한 경로와 하위 경로만 JWT 필터를 건너뛴다")
    void filtersPath(String path, boolean excluded) {
        when(request.getRequestURI()).thenReturn(path);

        assertThat(jwtFilter.shouldNotFilter(request)).isEqualTo(excluded);
    }
}
