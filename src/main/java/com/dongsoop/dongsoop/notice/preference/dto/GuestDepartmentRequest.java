package com.dongsoop.dongsoop.notice.preference.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record GuestDepartmentRequest(

        // 빈 Set은 유효한 값이다 (전체 구독 해지). null만 거부한다.
        @NotNull(message = "departmentTypes는 필수입니다.")
        Set<DepartmentType> departmentTypes
) {
}
