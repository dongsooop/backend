package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.oauth.dto.MemberSocialAccountDto;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Long id;

    private String accessToken;

    private String refreshToken;

    private String nickname;

    private String email;

    private DepartmentType departmentType;

    private List<RoleType> role;

    public LoginResponse(LoginMemberDetails loginMemberDetail, String accessToken, String refreshToken) {
        this.id = loginMemberDetail.getId();
        this.nickname = loginMemberDetail.getNickname();
        this.email = loginMemberDetail.getEmail();
        this.departmentType = loginMemberDetail.getDepartmentType();
        this.role = loginMemberDetail.getRole();

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public LoginResponse(MemberSocialAccountDto socialAccountDto, String accessToken, String refreshToken) {
        if (socialAccountDto == null) {
            throw new IllegalArgumentException("Social account cannot be null");
        }
        Member member = socialAccountDto.member();
        if (member == null) {
            throw new IllegalArgumentException("Member cannot be null");
        }

        this.id = member.getId();
        this.nickname = member.getNickname();
        this.email = member.getEmail();
        if (member.getDepartment() != null) {
            this.departmentType = member.getDepartment().getId();
        }
        this.role = socialAccountDto.roleType();

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
