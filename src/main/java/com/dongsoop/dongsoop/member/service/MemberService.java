package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.exception.domain.member.*;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.member.dto.LoginRequest;
import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    @Value("${jwt.header.name}")
    private String authHeaderName;

    @Value("${jwt.refreshToken.cookie.name}")
    private String refreshTokenCookieName;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    @Transactional
    public void signup(SignupRequest request) {

        checkEmailDuplication(request.getEmail());
        Member member = request.toEntity(passwordEncoder);
        memberRepository.save(member);
    }

    @Transactional
    public LoginResponse login(LoginRequest loginRequest, HttpServletResponse response) {

        Member member = memberRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(MemberNotFoundException::new);

        if (!passwordEncoder.matches(loginRequest.getPassword(), member.getPassword())) {
            throw new PasswordNotMatchedException();
        }

        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        String accessToken = tokenGenerator.generateAccessToken(authentication);
        String refreshToken = tokenGenerator.generateRefreshToken(authentication);

        // 액세스 토큰 헤더에 추가 (Authorization: Bearer xxx 형식으로)
        response.setHeader(authHeaderName, "Bearer " + accessToken);

        Cookie refreshCookie = new Cookie(refreshTokenCookieName, refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setMaxAge(604800); // 7일
        response.addCookie(refreshCookie);

        return new LoginResponse(accessToken);
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(MemberNotFoundException::new);
    }

    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(MemberNotFoundException::new);
    }

    private void checkEmailDuplication(String email) {
        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        optionalMember.ifPresent((member) -> {
            throw new EmailDuplicatedException();
        });
    }
}