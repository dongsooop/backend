package com.dongsoop.dongsoop.member.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.Arrays;
import java.util.List;
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

    private List<RoleType> role;

    public LoginMemberDetails(Long id, String nickname, String email, DepartmentType departmentType, String role) {
        this.id = id;
        this.nickname = nickname;
        this.email = email;
        this.departmentType = departmentType;
        this.role = parseRoles(role);
    }

    private List<RoleType> parseRoles(String role) {
        return Arrays.stream(role.split(","))
                .map(RoleType::valueOf)
                .toList();
    }
}
