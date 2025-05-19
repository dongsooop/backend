package com.dongsoop.dongsoop.member.controller;

import com.dongsoop.dongsoop.jwt.dto.TokenIssueResponse;
import com.dongsoop.dongsoop.member.dto.LoginDetails;
import com.dongsoop.dongsoop.member.dto.LoginRequest;
import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;
    @Value("${jwt.refreshToken.cookie.name}")
    private String refreshTokenCookieName;
    @Value("${jwt.expired-time.refresh-token}")
    private Long refreshTokenExpiredTime;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody @Valid SignupRequest signupRequest) {
        memberService.signup(signupRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                               HttpServletResponse response) {
        LoginDetails loginDetail = memberService.login(loginRequest);

        TokenIssueResponse issuedToken = loginDetail.getIssuedToken();
        String accessToken = issuedToken.getAccessToken();
        String refreshToken = issuedToken.getRefreshToken();

        setRefreshTokenCookie(response, refreshToken);

        LoginResponse loginResponse = new LoginResponse(loginDetail.getLoginMemberDetail(), accessToken);
        return ResponseEntity.ok(loginResponse);
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshCookie = new Cookie(refreshTokenCookieName, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) (refreshTokenExpiredTime / 1000));

        response.addCookie(refreshCookie);
    }
}
