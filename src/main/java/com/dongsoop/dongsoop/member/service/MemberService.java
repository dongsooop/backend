package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.exception.domain.member.*;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.entity.Role;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$";
    private static final String STUDENT_ID_REGEX = "^\\d{8}$";

    @Transactional
    public void signup(SignupRequest request) {
        checkEmailDuplication(request.getEmail());
        validateSignupRequest(request);

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .studentId(request.getStudentId())
                .department(request.getDepartment())
                .role(Role.USER)
                .build();

        memberRepository.save(member);
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
        memberRepository.findByEmail(email)
                .ifPresent(m -> {
                    throw new EmailDuplicatedException();
                });
    }

    private void validateSignupRequest(SignupRequest request) {
        validateEmail(request.getEmail());
        validatePassword(request.getPassword());
        validateStudentId(request.getStudentId());
    }

    private void validateEmail(String email) {
        boolean isValid = Pattern.matches(EMAIL_REGEX, email);

        if (!isValid) {
            throw new InvalidEmailFormatException();
        }
    }

    private void validatePassword(String password) {
        boolean isValid = Pattern.matches(PASSWORD_REGEX, password);

        if (!isValid) {
            throw new InvalidPasswordFormatException();
        }
    }

    private void validateStudentId(String studentId) {
        boolean isValid = Pattern.matches(STUDENT_ID_REGEX, studentId);

        if (!isValid) {
            throw new InvalidStudentIdFormatException();
        }
    }
}