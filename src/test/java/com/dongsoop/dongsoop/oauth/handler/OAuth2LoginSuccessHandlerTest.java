package com.dongsoop.dongsoop.oauth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.oauth.dto.CustomOAuth2User;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2LoginSuccessHandler 테스트")
class OAuth2LoginSuccessHandlerTest {
    private static final Long MEMBER_ID = 1L;
    private static final String SOCIAL_TOKEN = "social-token";
    private static final String TARGET_URL = "https://app.dongsoop.com/auth/callback";
    @Mock
    private TokenGenerator tokenGenerator;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private RedirectStrategy redirectStrategy;
    @InjectMocks
    private OAuth2LoginSuccessHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "targetUrl", TARGET_URL);
        handler.setRedirectStrategy(redirectStrategy);
    }

    @Test
    @DisplayName("OAuth2 로그인 성공 처리")
    void onAuthenticationSuccess() throws Exception {
        // given
        OAuth2User oAuth2User = mock(OAuth2User.class);
        CustomOAuth2User customUser = new CustomOAuth2User(
                oAuth2User,
                MEMBER_ID,
                List.of(RoleType.USER)
        );
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(customUser);
        when(tokenGenerator.generateSocialToken(any())).thenReturn(SOCIAL_TOKEN);

        // when
        handler.onAuthenticationSuccess(request, response, authentication);

        // then
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(any(), any(), urlCaptor.capture());

        String redirectUrl = urlCaptor.getValue();
        assertThat(redirectUrl).contains(TARGET_URL);
        assertThat(redirectUrl).contains("socialToken=" + SOCIAL_TOKEN);
        verify(tokenGenerator).generateSocialToken(any());
    }
}
