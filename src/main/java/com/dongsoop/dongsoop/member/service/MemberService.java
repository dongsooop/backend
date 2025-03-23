package com.dongsoop.dongsoop.member.service;

import com.dongsoop.dongsoop.exception.domain.member.*;
import com.dongsoop.dongsoop.member.dto.SignupRequest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.entity.Role;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequest request) {
        request.validate();

        checkEmailDuplication(request.getEmail());

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
        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        optionalMember.ifPresent((member) -> {
            throw new EmailDuplicatedException();
        });
    }
}