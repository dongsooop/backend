package com.dongsoop.dongsoop.service.member;

import com.dongsoop.dongsoop.dto.member.MemberResponseDto;
import com.dongsoop.dongsoop.dto.member.MemberSignupRequestDto;
import com.dongsoop.dongsoop.dto.member.MemberUpdateRequestDto;

public interface MemberService {
    MemberResponseDto signup(MemberSignupRequestDto requestDto);
    MemberResponseDto login(String email, String password);
    MemberResponseDto findByEmail(String email);
    MemberResponseDto findById(Long id);
    MemberResponseDto updateMember(Long id, MemberUpdateRequestDto requestDto);
    void deleteMember(Long id);

}