package com.dongsoop.dongsoop.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.oauth.controller.OAuth2Controller;
import com.dongsoop.dongsoop.oauth.dto.*;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.InvalidProviderTypeException;
import com.dongsoop.dongsoop.oauth.provider.*;
import com.dongsoop.dongsoop.oauth.service.OAuth2Service;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SocialLoginEntryContractTest {
    @Mock MemberService members;
    @Mock MemberDeviceService devices;
    @Mock OAuth2Service oauth;
    @Mock KakaoSocialProvider kakao;
    @Mock GoogleSocialProvider google;
    @Mock AppleSocialProvider apple;
    @Mock FCMService fcm;
    @InjectMocks OAuth2Controller controller;
    private final Authentication authentication = new UsernamePasswordAuthenticationToken(7L, null, List.of());
    private final LoginResponse response = new LoginResponse();

    @BeforeEach
    void configureTopic() {
        ReflectionTestUtils.setField(controller, "anonymousTopic", "anonymous-test");
    }

    @ParameterizedTest
    @EnumSource(OAuthProviderType.class)
    void eachProviderUsesMobileBindingAndReturnsIssuedResponse(OAuthProviderType provider) {
        stubLogin(provider, authentication);
        when(oauth.acceptLogin(authentication, 7L, "mobile-token")).thenReturn(response);
        var request = new SocialLoginRequest("provider-token", "mobile-token", MemberDeviceType.IOS);

        var actual = login(provider, request);

        assertThat(actual.getStatusCode().value()).isEqualTo(200);
        assertThat(actual.getBody()).isSameAs(response);
        verify(devices).bindDeviceWithMemberId(7L, "mobile-token");
        verify(fcm).unsubscribeTopic(List.of("mobile-token"), "anonymous-test");
        verify(devices, never()).createAndBindWebDevice(anyLong(), any());
    }

    @Test
    void webUsesEffectiveServerTokenWithoutMobileSubscription() {
        when(google.login("provider-token")).thenReturn(authentication);
        when(devices.createAndBindWebDevice(7L, "old-web")).thenReturn("effective-web");
        when(oauth.acceptLogin(authentication, 7L, "effective-web")).thenReturn(response);

        var actual = controller.googleLogin(new SocialLoginRequest("provider-token", "old-web", MemberDeviceType.WEB));

        assertThat(actual.getBody()).isSameAs(response);
        verifyNoInteractions(fcm);
        verify(devices, never()).bindDeviceWithMemberId(anyLong(), any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void absentMobileTokenFallsBackToWebDevice(String token) {
        when(apple.login("provider-token")).thenReturn(authentication);
        when(devices.createAndBindWebDevice(7L, token)).thenReturn("generated-web");
        when(oauth.acceptLogin(authentication, 7L, "generated-web")).thenReturn(response);

        assertThat(controller.appleLogin(new SocialLoginRequest("provider-token", token, MemberDeviceType.IOS)).getBody())
                .isSameAs(response);
        verifyNoInteractions(fcm);
    }

    @ParameterizedTest
    @EnumSource(OAuthProviderType.class)
    void invalidPrincipalCannotBindDeviceOrIssueTokens(OAuthProviderType provider) {
        stubLogin(provider, new UsernamePasswordAuthenticationToken("7", null));
        var request = new SocialLoginRequest("provider-token", "mobile-token", MemberDeviceType.IOS);

        assertThatThrownBy(() -> login(provider, request)).isInstanceOf(NotAuthenticationException.class);
        verifyNoInteractions(devices, oauth, fcm);
    }

    @Test
    void accountLinksUseAuthenticatedMemberAndReturnProviderTimestamp() {
        var request = new SocialAccountLinkRequest("link-token");
        var timestamp = LocalDateTime.of(2026, 10, 16, 12, 0);
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        when(google.linkSocialAccount(7L, request)).thenReturn(timestamp);
        when(kakao.linkSocialAccount(7L, request)).thenReturn(timestamp);
        when(apple.linkSocialAccount(7L, request)).thenReturn(timestamp);

        assertThat(controller.linkGoogleAccount(request).getBody()).isEqualTo(timestamp);
        assertThat(controller.linkKakaoAccount(request).getBody()).isEqualTo(timestamp);
        assertThat(controller.linkAppleAccount(request).getBody()).isEqualTo(timestamp);
    }

    @Test
    void unlinkAcceptsLowercaseProviderAndStateUsesAuthenticatedMember() {
        var request = new UnlinkSocialAccountRequest("unlink-token");
        var state = List.of(new MemberSocialAccountOverview(OAuthProviderType.GOOGLE, LocalDateTime.of(2026, 1, 1, 0, 0)));
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        when(oauth.getSocialAccountState(7L)).thenReturn(state);

        assertThat(controller.unlinkSocialAccount("google", request).getStatusCode().value()).isEqualTo(204);
        verify(oauth).unlinkMemberWithProviderType(7L, OAuthProviderType.GOOGLE, request);
        assertThat(controller.getSocialAccountState().getBody()).isEqualTo(state);
    }

    @Test
    void unknownProviderCannotUnlink() {
        var request = new UnlinkSocialAccountRequest("unlink-token");
        assertThatThrownBy(() -> controller.unlinkSocialAccount("unknown", request))
                .isInstanceOf(InvalidProviderTypeException.class);
        verifyNoInteractions(members, oauth);
    }

    @Test
    void appleCallbackReturnsConfiguredRedirect() {
        ReflectionTestUtils.setField(controller, "appleRedirectUri", "https://example.test/callback?code=%s&token=%s&package=%s&scheme=%s");
        ReflectionTestUtils.setField(controller, "packageId", "test.package");
        ReflectionTestUtils.setField(controller, "appleScheme", "testscheme");
        var actual = controller.appleCallback("code", "token");
        assertThat(actual.getStatusCode().value()).isEqualTo(302);
        assertThat(actual.getHeaders().getLocation()).hasToString("https://example.test/callback?code=code&token=token&package=test.package&scheme=testscheme");
    }

    private void stubLogin(OAuthProviderType provider, Authentication auth) {
        switch (provider) {
            case GOOGLE -> when(google.login("provider-token")).thenReturn(auth);
            case KAKAO -> when(kakao.login("provider-token")).thenReturn(auth);
            case APPLE -> when(apple.login("provider-token")).thenReturn(auth);
        }
    }

    private org.springframework.http.ResponseEntity<LoginResponse> login(OAuthProviderType provider, SocialLoginRequest request) {
        return switch (provider) {
            case GOOGLE -> controller.googleLogin(request);
            case KAKAO -> controller.kakaoLogin(request);
            case APPLE -> controller.appleLogin(request);
        };
    }
}
