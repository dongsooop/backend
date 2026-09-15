package com.dongsoop.dongsoop.eclass.dto;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record EclassAssignmentResponse(

        Long id,
        Long assignId,
        String courseName,
        String title,
        LocalDateTime dueAt,
        LocalDateTime cutoffAt,
        long dDay,
        boolean submitted,
        String link
) {

    public static EclassAssignmentResponse from(EclassAssignment assignment, LocalDate today, String baseUrl) {
        long dDay = ChronoUnit.DAYS.between(today, assignment.getDueAt().toLocalDate());

        return new EclassAssignmentResponse(
                assignment.getId(),
                assignment.getAssignId(),
                assignment.getCourseName(),
                assignment.getTitle(),
                assignment.getDueAt(),
                assignment.getCutoffAt(),
                dDay,
                assignment.isSubmitted(),
                link(baseUrl, assignment.getCourseModuleId()));
    }

    /**
     * 과제 상세 페이지 주소. 게시판 경로와 무관하게 코스모듈 번호만으로 열린다.
     */
    public static String link(String baseUrl, Long courseModuleId) {
        return baseUrl + "/mod/assign/view.php?id=" + courseModuleId;
    }
}
