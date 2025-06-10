package com.dongsoop.dongsoop.recruitment.tutoring.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.time.LocalDateTime;
import java.util.List;

public record TutoringBoardOverview(

        Long id,
        Integer volunteer,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String title,
        String content,
        String tags,
        List<DepartmentType> departmentTypeList
) {
    public TutoringBoardOverview(Long id, Integer volunteer, LocalDateTime startAt, LocalDateTime endAt, String title,
                                 String content, String tags, DepartmentType departmentType) {
        this(
                id,
                volunteer,
                startAt,
                endAt,
                title,
                content,
                tags,
                List.of(departmentType)
        );
    }
}
