package com.dongsoop.dongsoop.recruitment.tutoring.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
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
        Integer volunteer,
        RecruitmentViewType viewType,
        boolean isAlreadyApplied
) {
    public TutoringBoardDetails(Long id, String title, String content, String tags, LocalDateTime startAt,
                                LocalDateTime endAt, DepartmentType departmentType, String author,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt, Integer volunteer, RecruitmentViewType viewType,
                                boolean isAlreadyApplied) {
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
                volunteer,
                viewType,
                isAlreadyApplied
        );
    }
}
