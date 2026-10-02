package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.report.service.ReportService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * 신고 관리자 API 의 인가를 실제 보안 필터 체인(JwtFilter 포함)으로 검증한다.
 *
 * <p>메서드 보안이 활성화되어 있지 않아 컨트롤러의 {@code @Secured} 는 동작하지 않는다.
 * 관리자 경로의 유일한 방어선은 application.yml 의 {@code authentication.path.admin} 이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("신고 관리자 API 인가 통합 테스트")
class ReportAdminAuthorizationTest {

    // App Check 필터가 먼저 401 을 내면 인증 실패와 구분되지 않으므로 모든 요청에 붙인다
    private static final String APP_CHECK_HEADER = "X-Firebase-AppCheck";
    private static final String APP_CHECK_TOKEN = "app-check-token";

    private static final String SERVER_MESSAGE_REPORT_BODY = """
            {
              "reportType": "BLINDDATE_MESSAGE",
              "reporterId": 11,
              "targetMemberId": 42,
              "roomId": "session-1",
              "messageId": "m1",
              "messageContent": "신고된 메시지",
              "messageSentAt": "2026-09-28T21:13:40",
              "reason": "HATE_SPEECH"
            }
            """;

    private static final List<Supplier<MockHttpServletRequestBuilder>> ADMIN_REQUESTS = List.of(
            () -> get("/reports/admin"),
            () -> post("/reports/message")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SERVER_MESSAGE_REPORT_BODY),
            () -> post("/reports/sanctions").contentType(MediaType.APPLICATION_JSON).content("{}"),
            () -> post("/reports/7/dismiss")
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenGenerator tokenGenerator;

    @MockitoBean
    private DeviceBlacklistService deviceBlacklistService;

    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;

    @MockitoBean
    private ReportService reportService;

    @Test
    @DisplayName("USER 토큰으로 관리자 신고 API를 호출하면 403을 반환한다")
    void userToken_OnAdminPaths_ReturnsForbidden() throws Exception {
        String token = accessToken(RoleType.USER_ROLE);

        for (Supplier<MockHttpServletRequestBuilder> request : ADMIN_REQUESTS) {
            mockMvc.perform(request.get()
                            .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("토큰 없이 관리자 신고 API를 호출하면 401을 반환한다")
    void noToken_OnAdminPaths_ReturnsUnauthorized() throws Exception {
        for (Supplier<MockHttpServletRequestBuilder> request : ADMIN_REQUESTS) {
            mockMvc.perform(request.get().header(APP_CHECK_HEADER, APP_CHECK_TOKEN))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("ADMIN 토큰은 관리자 신고 API의 인가를 통과한다")
    void adminToken_OnAdminPaths_PassesAuthorization() throws Exception {
        String token = accessToken(RoleType.ADMIN_ROLE);

        for (Supplier<MockHttpServletRequestBuilder> request : ADMIN_REQUESTS) {
            mockMvc.perform(request.get()
                            .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(result -> assertThat(result.getResponse().getStatus())
                            .isNotIn(401, 403));
        }
    }

    @Test
    @DisplayName("ADMIN 토큰으로 서버 간 메시지 신고를 보내면 201을 반환한다")
    void adminToken_OnServerMessageReport_ReturnsCreated() throws Exception {
        mockMvc.perform(post("/reports/message")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header("Authorization", "Bearer " + accessToken(RoleType.ADMIN_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SERVER_MESSAGE_REPORT_BODY))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("USER 토큰으로 사용자 채팅 신고 경로를 호출하면 403이 아니다")
    void userToken_OnChatReport_IsNotForbidden() throws Exception {
        mockMvc.perform(post("/reports/chat")
                        .header(APP_CHECK_HEADER, APP_CHECK_TOKEN)
                        .header("Authorization", "Bearer " + accessToken(RoleType.USER_ROLE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isNotIn(401, 403));
    }

    private String accessToken(String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority(role)));
        return tokenGenerator.generateAccessToken(auth, 1L);
    }
}
