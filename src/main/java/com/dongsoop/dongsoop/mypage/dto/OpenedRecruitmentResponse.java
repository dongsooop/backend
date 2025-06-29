package com.dongsoop.dongsoop.mypage.dto;

import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record OpenedRecruitmentResponse(

        Long id,
        Long volunteer,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String title,
        String content,
        List<String> tags,
        List<String> departmentTypeList,
        RecruitmentType boardType,
        LocalDateTime createdAt,
        boolean isRecruiting
) {
    public OpenedRecruitmentResponse(OpenedRecruitment openedRecruitment) {
        this(
                openedRecruitment.getId(),
                openedRecruitment.getVolunteer(),
                openedRecruitment.getStartAt(),
                openedRecruitment.getEndAt(),
                openedRecruitment.getTitle(),
                openedRecruitment.getContent(),
                splitedTag(openedRecruitment.getTags()),
                splitedDepartmentType(openedRecruitment.getDepartmentTypeList()),
                openedRecruitment.getBoardType(),
                openedRecruitment.getCreatedAt(),
                openedRecruitment.getIsRecruiting()
        );
    }

    private static List<String> splitedTag(String tags) {
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .toList();
    }

    private static List<String> splitedDepartmentType(String departmentTypes) {
        return Arrays.stream(departmentTypes.split(","))
                .map(v -> v.trim())
                .toList();
    }
}
