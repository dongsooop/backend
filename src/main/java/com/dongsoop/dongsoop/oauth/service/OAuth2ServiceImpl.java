package com.dongsoop.dongsoop.oauth.service;

import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.dto.LoginResponse;
import com.dongsoop.dongsoop.member.exception.MemberNotFoundException;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    private final MemberRepository memberRepository;
    private final TokenGenerator tokenGenerator;

    @Override
    public LoginResponse acceptLogin(Long memberId) {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();

        String accessToken = tokenGenerator.generateAccessToken(authentication);
        String refreshToken = tokenGenerator.generateRefreshToken(authentication);

        LoginMemberDetails loginMemberDetails = memberRepository.findLoginMemberDetailById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        return new LoginResponse(loginMemberDetails, accessToken, refreshToken);
    }
}
