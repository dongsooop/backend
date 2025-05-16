package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private Long id;

    private String accessToken;

    private String nickname;

    private String email;

    private DepartmentType departmentType;

    public LoginResponse(LoginMemberDetails loginMemberDetail, String accessToken) {
        this.id = loginMemberDetail.getId();
        this.nickname = loginMemberDetail.getNickname();
        this.email = loginMemberDetail.getEmail();
        this.departmentType = loginMemberDetail.getDepartmentType();

        this.accessToken = accessToken;
    }
}
