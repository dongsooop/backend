package com.dongsoop.dongsoop.mypage.dto;

import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record MyRecruitmentOverviewResponse(

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
        RecruitmentStatusType status
) {
    public MyRecruitmentOverviewResponse(MyRecruitmentOverview openedRecruitment) {
        this(
                openedRecruitment.getId(),
                openedRecruitment.getVolunteer(),
                openedRecruitment.getStartAt(),
                openedRecruitment.getEndAt(),
                openedRecruitment.getTitle(),
                openedRecruitment.getContent(),
                splitTags(openedRecruitment.getTags()),
                splitDepartmentType(openedRecruitment.getDepartmentTypeList()),
                openedRecruitment.getBoardType(),
                openedRecruitment.getCreatedAt(),
                openedRecruitment.getStatus()
        );
    }

    private static List<String> splitTags(String tags) {
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .toList();
    }

    private static List<String> splitDepartmentType(String departmentTypes) {
        return Arrays.stream(departmentTypes.split(","))
                .map(String::trim)
                .toList();
    }
}
