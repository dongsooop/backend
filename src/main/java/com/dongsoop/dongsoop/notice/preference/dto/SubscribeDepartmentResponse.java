package com.dongsoop.dongsoop.notice.preference.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.util.List;

public record SubscribeDepartmentResponse(

        List<DepartmentType> departmentTypes
) {
}
