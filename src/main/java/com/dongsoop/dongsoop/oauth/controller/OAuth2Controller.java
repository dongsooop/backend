package com.dongsoop.dongsoop.oauth.controller;

import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.oauth.dto.OAuthLoginRequest;
import com.dongsoop.dongsoop.oauth.service.OAuth2Service;
import com.dongsoop.dongsoop.role.entity.RoleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final MemberService memberService;
    private final MemberDeviceService memberDeviceService;
    private final OAuth2Service oAuth2Service;

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

    // TODO: OAuth 계정 연동
}
