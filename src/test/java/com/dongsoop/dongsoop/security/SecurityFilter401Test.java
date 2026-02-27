package com.dongsoop.dongsoop.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.dongsoop.dongsoop.appcheck.FirebaseAppCheck;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.jwt.exception.BlacklistedTokenException;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@DisplayName("보안 필터 체인 401 응답 통합 테스트")
class SecurityFilter401Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenGenerator tokenGenerator;

    @MockitoBean
    private DeviceBlacklistService deviceBlacklistService;

    @MockitoBean
    private FirebaseAppCheck firebaseAppCheck;

    @Test
    @DisplayName("토큰 없이 보호된 경로 요청 시 401을 반환한다")
    void when_no_token_on_protected_path_then_401() throws Exception {
        mockMvc.perform(get("/device/list"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("형식이 잘못된 토큰으로 보호된 경로 요청 시 401을 반환한다")
    void when_malformed_token_on_protected_path_then_401() throws Exception {
        mockMvc.perform(get("/device/list")
                        .header("Authorization", "Bearer invalid.malformed.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("강제 로그아웃된 디바이스의 토큰으로 보호된 경로 요청 시 401을 반환한다")
    void when_blacklisted_token_on_protected_path_then_401() throws Exception {
        String token = generateAccessToken(1L, 42L);
        doThrow(new BlacklistedTokenException())
                .when(deviceBlacklistService).validateNotBlacklisted(any(), any());

        mockMvc.perform(get("/device/list")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("강제 로그아웃된 디바이스의 토큰으로 /logout 요청 시 401을 반환한다")
    void when_blacklisted_token_on_logout_then_401() throws Exception {
        String token = generateAccessToken(1L, 42L);
        doThrow(new BlacklistedTokenException())
                .when(deviceBlacklistService).validateNotBlacklisted(any(), any());

        mockMvc.perform(post("/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String generateAccessToken(Long memberId, Long deviceId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                memberId, null,
                List.of(new SimpleGrantedAuthority(RoleType.USER_ROLE)));
        return tokenGenerator.generateAccessToken(auth, deviceId);
    }
}
