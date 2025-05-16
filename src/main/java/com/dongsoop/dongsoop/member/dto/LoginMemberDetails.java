package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginMemberDetails {

    private Long id;

    private String nickname;

    private String email;

    private DepartmentType departmentType;
}
