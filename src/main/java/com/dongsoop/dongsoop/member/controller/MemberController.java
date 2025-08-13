package com.dongsoop.dongsoop.member.controller;

import com.dongsoop.dongsoop.jwt.dto.IssuedToken;
import com.dongsoop.dongsoop.member.dto.EmailValidateRequest;
import com.dongsoop.dongsoop.member.dto.LoginDetails;
import com.dongsoop.dongsoop.member.dto.LoginRequest;
import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.dto.NicknameValidateRequest;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.dto.UpdatePasswordRequest;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.member.validate.MemberDuplicationValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/password")
    public ResponseEntity<Void> updatePassword(@RequestBody @Valid UpdatePasswordRequest request) {
        memberService.updatePassword(request);

        return ResponseEntity.noContent()
                .build();
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@RequestBody @Valid SignupRequest signupRequest) {
        memberService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginDetails loginDetail = memberService.login(loginRequest);

        IssuedToken issuedToken = loginDetail.getIssuedToken();
        String accessToken = issuedToken.getAccessToken();
        String refreshToken = issuedToken.getRefreshToken();

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
