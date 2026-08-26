package com.dongsoop.dongsoop.notice.preference.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import jakarta.validation.constraints.NotNull;

public record GuestDepartmentRequest(

        @NotNull(message = "학과는 필수 입력값입니다.")
        DepartmentType departmentType
) {
}
