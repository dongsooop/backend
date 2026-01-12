package com.dongsoop.dongsoop.oauth.controller;

import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.oauth.dto.OAuthLoginRequest;
import com.dongsoop.dongsoop.oauth.dto.SocialLoginRequest;
import com.dongsoop.dongsoop.oauth.provider.AppleSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.GoogleSocialProvider;
import com.dongsoop.dongsoop.oauth.provider.KakaoSocialProvider;
import com.dongsoop.dongsoop.oauth.service.OAuth2Service;
import com.dongsoop.dongsoop.role.entity.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
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

        // 알림 구독 설정
        memberDeviceService.bindDeviceWithMemberId(memberId, request.deviceToken());

        // 로그인 시 필요한 데이터 생성
        LoginResponse loginResponse = oAuth2Service.acceptLogin(memberId);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@RequestBody SocialLoginRequest request) {
        // 서비스 로직 호출
        Long memberId = this.kakaoSocialProvider.login(request.token());

        LoginResponse response = oAuth2Service.acceptLogin(memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@RequestBody SocialLoginRequest request) {
        // 서비스 로직 호출
        Long memberId = this.googleSocialProvider.login(request.token());

        LoginResponse response = oAuth2Service.acceptLogin(memberId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apple")
    public ResponseEntity<LoginResponse> appleLogin(@RequestBody SocialLoginRequest request) {
        // 서비스 로직 호출
        Long memberId = this.appleSocialProvider.login(request.token());

        LoginResponse response = oAuth2Service.acceptLogin(memberId);
        return ResponseEntity.ok(response);
    }

    // TODO: OAuth 계정 연동
}
