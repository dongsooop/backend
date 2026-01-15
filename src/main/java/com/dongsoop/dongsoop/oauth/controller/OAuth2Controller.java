package com.dongsoop.dongsoop.oauth.controller;

import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountOverview;
import com.dongsoop.dongsoop.oauth.dto.OAuthLoginRequest;
import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import com.dongsoop.dongsoop.oauth.dto.SocialLoginRequest;
import com.dongsoop.dongsoop.oauth.dto.UnlinkSocialAccountRequest;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.exception.InvalidProviderTypeException;
import com.dongsoop.dongsoop.oauth.provider.AppleSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.GoogleSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.KakaoSocialProvider;
import com.dongsoop.dongsoop.oauth.service.OAuth2Service;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final MemberService memberService;
    private final MemberDeviceService memberDeviceService;
    private final OAuth2Service oAuth2Service;
    private final KakaoSocialProvider kakaoSocialProvider;
    private final GoogleSocialProvider googleSocialProvider;
    private final AppleSocialProvider appleSocialProvider;
    private final FCMService fcmService;

    @Value("${oauth.apple.redirect-uri}")
    private String appleRedirectUri;

    @Value("${oauth.apple.package}")
    private String packageId;

    @Value("${oauth.apple.scheme}")
    private String appleScheme;

    @Value("${notification.topic.anonymous}")
    private String anonymousTopic;

    // 임시 발급한 토큰으로 검증
    @PostMapping("/login")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<LoginResponse> acceptLogin(@RequestBody @Valid OAuthLoginRequest request) {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        Long memberId = this.getMemberIdByAuthentication(authentication);

        // 알림 구독 설정
        memberDeviceService.bindDeviceWithMemberId(memberId, request.deviceToken());
        this.unsubscribeAnonymous(request.deviceToken());

        // 로그인 시 필요한 데이터 생성
        LoginResponse loginResponse = oAuth2Service.acceptLogin(authentication, memberId);

        return ResponseEntity.ok(loginResponse);
    }

    // 애플 로그인 리다이렉트 방식 콜백
    @PostMapping("/apple/callback")
    public ResponseEntity<Void> appleCallback(
            @RequestParam("code") String code,
            @RequestParam("id_token") String idToken) {
        String redirectUri = String.format(appleRedirectUri,
                code,
                idToken,
                packageId,
                appleScheme);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUri))
                .build();
    }

    @PostMapping("/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestBody @Valid SocialLoginRequest request) {
        Authentication authentication = this.kakaoSocialProvider.login(request.token());
        Long memberId = this.getMemberIdByAuthentication(authentication);

        memberDeviceService.bindDeviceWithMemberId(memberId, request.deviceToken());
        this.unsubscribeAnonymous(request.deviceToken());

        LoginResponse response = oAuth2Service.acceptLogin(authentication, memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@RequestBody @Valid SocialLoginRequest request) {
        Authentication authentication = this.googleSocialProvider.login(request.token());
        Long memberId = this.getMemberIdByAuthentication(authentication);

        memberDeviceService.bindDeviceWithMemberId(memberId, request.deviceToken());
        this.unsubscribeAnonymous(request.deviceToken());

        LoginResponse response = oAuth2Service.acceptLogin(authentication, memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apple")
    public ResponseEntity<LoginResponse> appleLogin(@RequestBody @Valid SocialLoginRequest request) {
        Authentication authentication = this.appleSocialProvider.login(request.token());
        Long memberId = this.getMemberIdByAuthentication(authentication);
        memberDeviceService.bindDeviceWithMemberId(memberId, request.deviceToken());
        this.unsubscribeAnonymous(request.deviceToken());

        LoginResponse response = oAuth2Service.acceptLogin(authentication, memberId);
        return ResponseEntity.ok(response);
    }

    private Long getMemberIdByAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Long memberId)) {
            throw new NotAuthenticationException();
        }

        return memberId;
    }

    @PostMapping("/link/google")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<LocalDateTime> linkGoogleAccount(@RequestBody @Valid SocialAccountLinkRequest request) {
        Long memberId = this.memberService.getMemberIdByAuthentication();

        LocalDateTime createdAt = googleSocialProvider.linkSocialAccount(memberId, request);

        return ResponseEntity.ok(createdAt);
    }

    @PostMapping("/link/kakao")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<LocalDateTime> linkKakaoAccount(@RequestBody @Valid SocialAccountLinkRequest request) {
        Long memberId = this.memberService.getMemberIdByAuthentication();

        LocalDateTime createdAt = kakaoSocialProvider.linkSocialAccount(memberId, request);

        return ResponseEntity.ok(createdAt);
    }

    @PostMapping("/link/apple")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<LocalDateTime> linkAppleAccount(@RequestBody @Valid SocialAccountLinkRequest request) {
        Long memberId = this.memberService.getMemberIdByAuthentication();

        LocalDateTime createdAt = appleSocialProvider.linkSocialAccount(memberId, request);

        return ResponseEntity.ok(createdAt);
    }

    @DeleteMapping("/{providerType}")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<Void> unlinkSocialAccount(@PathVariable @NotBlank String providerType,
                                                    @RequestBody @Valid UnlinkSocialAccountRequest request) {

        try {
            OAuthProviderType oAuthProviderType = OAuthProviderType.valueOf(providerType.toUpperCase());

            Long memberId = this.memberService.getMemberIdByAuthentication();
            oAuth2Service.unlinkMemberWithProviderType(memberId, oAuthProviderType, request);

            return ResponseEntity.noContent()
                    .build();
        } catch (IllegalArgumentException e) {
            throw new InvalidProviderTypeException(providerType);
        }
    }

    @GetMapping("/state")
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<List<MemberSocialAccountOverview>> getSocialAccountState() {
        List<MemberSocialAccountOverview> socialAccountState = this.oAuth2Service.getSocialAccountState(
                memberService.getMemberIdByAuthentication());

        return ResponseEntity.ok(socialAccountState);
    }

    private void unsubscribeAnonymous(String deviceToken) {
        fcmService.unsubscribeTopic(List.of(deviceToken), anonymousTopic);
    }
}
