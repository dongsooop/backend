package com.dongsoop.dongsoop.oauth.controller;

import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountOverview;
import com.dongsoop.dongsoop.oauth.dto.OAuthLoginRequest;
import com.dongsoop.dongsoop.oauth.dto.SocialAccountLinkRequest;
import com.dongsoop.dongsoop.oauth.dto.SocialLoginRequest;
import com.dongsoop.dongsoop.oauth.entity.OAuthProviderType;
import com.dongsoop.dongsoop.oauth.provider.AppleSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.GoogleSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.KakaoSocialProvider;
import com.dongsoop.dongsoop.oauth.service.OAuth2Service;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    @GetMapping
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<LoginResponse> acceptLogin(OAuthLoginRequest request) {
        Long memberId = this.memberService.getMemberIdByAuthentication();
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        // 알림 구독 설정
        memberDeviceService.bindDeviceWithMemberId(memberId, request.deviceToken());

        // 로그인 시 필요한 데이터 생성
        LoginResponse loginResponse = oAuth2Service.acceptLogin(authentication, memberId);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestBody @Valid SocialLoginRequest request) {
        Long memberId = this.memberService.getMemberIdByAuthentication();
        Authentication authentication = this.kakaoSocialProvider.login(request.token());

        LoginResponse response = oAuth2Service.acceptLogin(authentication, memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@RequestBody @Valid SocialLoginRequest request) {
        Long memberId = this.memberService.getMemberIdByAuthentication();
        Authentication authentication = this.googleSocialProvider.login(request.token());

        LoginResponse response = oAuth2Service.acceptLogin(authentication, memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apple")
    public ResponseEntity<LoginResponse> appleLogin(@RequestBody @Valid SocialLoginRequest request) {
        Long memberId = this.memberService.getMemberIdByAuthentication();
        Authentication authentication = this.appleSocialProvider.login(request.token());

        LoginResponse response = oAuth2Service.acceptLogin(authentication, memberId);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<Void> withdrawMember(@PathVariable OAuthProviderType providerType) {
        Long memberId = this.memberService.getMemberIdByAuthentication();
        oAuth2Service.withdrawMemberWithProviderType(memberId, providerType);

        return ResponseEntity.noContent()
                .build();
    }

    @GetMapping
    @Secured(RoleType.USER_ROLE)
    public ResponseEntity<List<MemberSocialAccountOverview>> getSocialAccountState() {
        List<MemberSocialAccountOverview> socialAccountState = this.oAuth2Service.getSocialAccountState(
                memberService.getMemberIdByAuthentication());

        return ResponseEntity.ok(socialAccountState);
    }
}
