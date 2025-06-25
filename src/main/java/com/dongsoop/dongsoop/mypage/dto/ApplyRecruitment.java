package com.dongsoop.dongsoop.mypage.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record ApplyRecruitment(

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
    public ApplyRecruitment(Long id, Integer volunteer, LocalDateTime startAt, LocalDateTime endAt, String title,
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
                Arrays.stream(departmentTypes.split(","))
                        .map(v -> DepartmentType.valueOf(v.trim()))
                        .toList(),
                boardType,
                createdAt,
                isRecruiting
        );
    }
}
