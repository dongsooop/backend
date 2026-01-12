package com.dongsoop.dongsoop.oauth.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.role.entity.RoleType;
import java.util.List;

public record OAuthLoginResponse(

        Long id,

        String nickname,

        String email,

        DepartmentType departmentType,

        List<RoleType> role
) {
}
