package com.dongsoop.dongsoop.recruitment.tutoring.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.time.LocalDateTime;
import java.util.List;

public record TutoringBoardDetails(

        Long id,
        String title,
        String content,
        String tags,
        LocalDateTime startAt,
        LocalDateTime endAt,
        List<DepartmentType> departmentTypeList,
        String author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer volunteer
) {
    public TutoringBoardDetails(Long id, String title, String content, String tags, LocalDateTime startAt,
                                LocalDateTime endAt, DepartmentType departmentType, String author,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt, Integer volunteer) {
        this(
                id,
                title,
                content,
                tags,
                startAt,
                endAt,
                List.of(departmentType),
                author,
                createdAt,
                updatedAt,
                volunteer
        );
    }
}
