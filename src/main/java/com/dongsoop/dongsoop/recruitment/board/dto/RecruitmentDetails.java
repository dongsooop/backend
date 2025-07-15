package com.dongsoop.dongsoop.recruitment.board.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record RecruitmentDetails(

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
    public RecruitmentDetails(Long id, String title, String content, String tags, LocalDateTime startAt,
                              LocalDateTime endAt, String departmentTypes, String author, LocalDateTime createdAt,
                              LocalDateTime updatedAt, Integer volunteer, RecruitmentViewType viewType,
                              boolean isAlreadyApplied) {
        this(
                id,
                title,
                content,
                tags,
                startAt,
                endAt,
                getDepartmentTypeList(departmentTypes),
                author,
                createdAt,
                updatedAt,
                volunteer,
                viewType,
                isAlreadyApplied
        );
    }

    private static List<DepartmentType> getDepartmentTypeList(String departmentTypes) {
        return Arrays.stream(departmentTypes.split(","))
                .map(String::trim)
                .map(DepartmentType::valueOf)
                .distinct()
                .toList();
    }
}
