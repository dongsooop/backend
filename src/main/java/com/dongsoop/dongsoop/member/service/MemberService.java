package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.exception.domain.member.EmailDuplicatedException;
import com.dongsoop.dongsoop.exception.domain.member.MemberNotFoundException;
import com.dongsoop.dongsoop.jwt.TokenGenerator;
import com.dongsoop.dongsoop.jwt.dto.TokenIssueResponse;
import com.dongsoop.dongsoop.member.dto.LoginRequest;
import com.dongsoop.dongsoop.member.dto.PasswordValidateDto;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    private final UserDetailsService userDetailsService;

    @Transactional
    public void signup(SignupRequest request) {
        checkEmailDuplication(request.getEmail());
        Member member = request.toEntity(passwordEncoder);
        memberRepository.save(member);
    }

    @Transactional
    public TokenIssueResponse login(LoginRequest loginRequest) {
        Optional<PasswordValidateDto> passwordValidator = memberRepository.findPasswordValidatorByEmail(
                loginRequest.getEmail());

        if (passwordValidator.isEmpty()) {
            throw new MemberNotFoundException();
        }

        validatePassword(loginRequest, passwordValidator.get());

        String cryptedPassword = passwordEncoder.encode(loginRequest.getPassword());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(), cryptedPassword);

        String accessToken = tokenGenerator.generateAccessToken(authentication);
        String refreshToken = tokenGenerator.generateRefreshToken(authentication);

        return new TokenIssueResponse(accessToken, refreshToken);
    }

    private void validatePassword(LoginRequest requestPassword, PasswordValidateDto passwordValidateDto) {
        if (!passwordEncoder.matches(requestPassword.getPassword(), passwordValidateDto.getPassword())) {
            throw new MemberNotFoundException();
        }
    }

    private void checkEmailDuplication(String email) {
        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        optionalMember.ifPresent((member) -> {
            throw new EmailDuplicatedException();
        });
    }
}
