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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenGenerator tokenGenerator;

    public void signup(SignupRequest request) {
        checkEmailDuplication(request.getEmail());
        Member member = request.toEntity(passwordEncoder);
        memberRepository.save(member);
    }

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
        boolean isExists = memberRepository.existsByEmail(email);

        if (isExists) {
            throw new EmailDuplicatedException();
        }
    }
}
