package com.dongsoop.dongsoop.mypage.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record OpenedRecruitment(

        Long id,
        Integer volunteer,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String title,
        String content,
        String tags,
        List<DepartmentType> departmentTypeList,
        RecruitmentType boardType,
        LocalDateTime createdAt,
        boolean isRecruiting
) {
    public OpenedRecruitment(Long id, Integer volunteer, LocalDateTime startAt, LocalDateTime endAt, String title,
                             String content, String tags, String departmentTypes, RecruitmentType boardType,
                             LocalDateTime createdAt, boolean isRecruiting) {
        this(
                id,
                volunteer,
                startAt,
                endAt,
                title,
                content,
                tags,
                getDepartmentTypeList(departmentTypes),
                boardType,
                createdAt,
                isRecruiting
        );
    }

    private static List<DepartmentType> getDepartmentTypeList(String departmentTypes) {
        return Arrays.stream(departmentTypes.split(","))
                .map(String::trim)
                .map(DepartmentType::valueOf)
                .toList();
    }
}
