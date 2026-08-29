package com.dongsoop.dongsoop.notice.preference.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record GuestDepartmentRequest(

        @NotEmpty(message = "학과는 최소 1개 이상 선택해야 합니다.")
        Set<DepartmentType> departmentTypes
) {
}
