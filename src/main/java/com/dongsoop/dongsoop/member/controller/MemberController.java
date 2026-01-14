package com.dongsoop.dongsoop.member.controller;

import com.dongsoop.dongsoop.jwt.dto.IssuedToken;
import com.dongsoop.dongsoop.mailverify.passwordupdate.PasswordUpdateMailValidator;
import com.dongsoop.dongsoop.mailverify.register.RegisterMailValidator;
import com.dongsoop.dongsoop.member.dto.EmailValidateRequest;
import com.dongsoop.dongsoop.member.dto.LoginDetails;
import com.dongsoop.dongsoop.member.dto.LoginRequest;
import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.dto.NicknameValidateRequest;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.dto.UpdatePasswordRequest;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.member.validate.MemberDuplicationValidator;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.FCMService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final MemberDuplicationValidator memberDuplicationValidator;
    private final PasswordUpdateMailValidator passwordUpdateMailValidator;
    private final RegisterMailValidator registerMailValidator;
    private final MemberDeviceService memberDeviceService;
    private final FCMService fcmService;

    @Value("${notification.topic.anonymous}")
    private String anonymousTopic;

    @PostMapping("/password")
    public ResponseEntity<Void> updatePassword(@RequestBody @Valid UpdatePasswordRequest request) {
        passwordUpdateMailValidator.validateVerifySuccess(request.email());
        memberService.updatePassword(request);
        passwordUpdateMailValidator.removeVerificationCode(request.email());

        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequest signupRequest) {
        registerMailValidator.validateVerifySuccess(signupRequest.getEmail());
        memberService.signup(signupRequest);
        registerMailValidator.removeVerificationCode(signupRequest.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginDetails loginDetail = memberService.login(loginRequest);

        IssuedToken issuedToken = loginDetail.getIssuedToken();
        String accessToken = issuedToken.getAccessToken();
        String refreshToken = issuedToken.getRefreshToken();

        if (StringUtils.hasText(loginRequest.getFcmToken())) {
            memberDeviceService.bindDeviceWithMemberId(
                    loginDetail.getLoginMemberDetail().getId(),
                    loginRequest.getFcmToken());
            fcmService.unsubscribeTopic(List.of(loginRequest.getFcmToken()), anonymousTopic);
        }

        LoginResponse loginResponse = new LoginResponse(loginDetail.getLoginMemberDetail(), accessToken, refreshToken);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/validate/email")
    public ResponseEntity<Void> validateEmail(@RequestBody @Valid EmailValidateRequest request) {
        memberDuplicationValidator.validateEmailDuplication(request.email());
        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/validate/nickname")
    public ResponseEntity<Void> validateNickname(@RequestBody @Valid NicknameValidateRequest request) {
        memberDuplicationValidator.validateNicknameDuplication(request.nickname());
        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteMember() {
        memberService.deleteMember();
        return ResponseEntity.noContent()
                .build();
    }
}
