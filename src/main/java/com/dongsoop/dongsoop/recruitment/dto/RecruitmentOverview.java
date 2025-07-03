package com.dongsoop.dongsoop.recruitment.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record RecruitmentOverview(

        Long id,
        Integer volunteer,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String title,
        String content,
        String tags,
        List<DepartmentType> departmentTypeList
) {
    public RecruitmentOverview(Long id, Integer volunteer, LocalDateTime startAt, LocalDateTime endAt, String title,
                               String content, String tags, String departmentTypes) {
        this(
                id,
                volunteer,
                startAt,
                endAt,
                title,
                content,
                tags,
                Arrays.stream(departmentTypes.split(","))
                        .map(v -> DepartmentType.valueOf(v.trim()))
                        .distinct()
                        .toList()
        );
    }
}
