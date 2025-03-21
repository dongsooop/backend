package com.dongsoop.dongsoop.service.member;

import com.dongsoop.dongsoop.dto.member.MemberResponseDto;
import com.dongsoop.dongsoop.dto.member.MemberSignupRequestDto;
import com.dongsoop.dongsoop.dto.member.MemberUpdateRequestDto;
import com.dongsoop.dongsoop.entity.member.Member;
import com.dongsoop.dongsoop.entity.member.MemberMapper;
import com.dongsoop.dongsoop.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    @Override
    @Transactional
    public MemberResponseDto signup(MemberSignupRequestDto requestDto) {
        if (memberRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        Member member = memberMapper.toEntity(requestDto);
        Member savedMember = memberRepository.save(member);
        return memberMapper.toResponseDto(savedMember);
    }

    public MemberResponseDto login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 회원이 존재하지 않습니다."));
        if (!member.getPassword().equals(password)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        return memberMapper.toResponseDto(member);
    }

    @Override
    public MemberResponseDto findByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 회원이 존재하지 않습니다."));
        return memberMapper.toResponseDto(member);
    }

    @Override
    public MemberResponseDto findById(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 회원이 존재하지 않습니다."));
        return memberMapper.toResponseDto(member);
    }

    @Override
    @Transactional
    public MemberResponseDto updateMember(Long id, MemberUpdateRequestDto requestDto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 ID의 회원이 존재하지 않습니다."));

        memberMapper.updateMemberFromDto(requestDto, member);
        return memberMapper.toResponseDto(member);
    }

    @Override
    @Transactional
    public void deleteMember(Long id) {
        if (!memberRepository.existsById(id)) {
            throw new IllegalArgumentException("해당 ID의 회원이 존재하지 않습니다.");
        }
        memberRepository.deleteById(id);
    }
}