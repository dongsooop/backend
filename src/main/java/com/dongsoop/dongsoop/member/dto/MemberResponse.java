package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberResponse {
    private Long id;
    private String email;
    private String nickname;
    private String studentId;
    private String department;

    public static MemberResponse from(Member member) {
        return MemberResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .studentId(member.getStudentId())
                .department(member.getDepartment())
                .build();
    }
}