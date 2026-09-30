package com.dongsoop.dongsoop.blinddate.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.blinddate.service.BlindDateService;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 메서드 보안이 꺼져 있어 {@code @Secured} 는 동작하지 않는다.
 * 세션 강제 종료 API 의 방어선은 application.yml 의 {@code authentication.path.admin} 뿐이므로 실제 필터 체인으로 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("과팅 세션 강제 종료 API 인가 통합 테스트")
class BlindDateResetAuthorizationTest {

    // App Check 필터가 먼저 401 을 내면 인가 실패와 구분되지 않으므로 모든 요청에 붙인다
    private static final String APP_CHECK_HEADER = "X-Firebase-AppCheck";
    private static final String APP_CHECK_TOKEN = "app-check-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenGenerator tokenGenerator;

    @MockitoBean
    private DeviceBlacklistService deviceBlacklistService;

    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;

    @MockitoBean
    private BlindDateService blindDateService;

    @Test
    @DisplayName("USER 토큰으로 호출하면 403을 반환한다")
    void userToken_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/blinddate/reset").param("sessionId", "session-1")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header("Authorization", "Bearer " + accessToken(RoleType.USER_ROLE)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("토큰 없이 호출하면 401을 반환한다")
    void noToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/blinddate/reset").param("sessionId", "session-1")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 토큰으로 호출하면 세션을 닫고 204를 반환한다")
    void adminToken_ResetsAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/blinddate/reset").param("sessionId", "session-1")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header("Authorization", "Bearer " + accessToken(RoleType.ADMIN_ROLE)))
                .andExpect(status().isNoContent());

        verify(blindDateService).closeSession("session-1");
    }

    private String accessToken(String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority(role)));
        return tokenGenerator.generateAccessToken(auth, 1L);
    }
}
