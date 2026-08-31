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
                EclassAssignmentLink.of(baseUrl, assignment.getCourseModuleId()));
    }
}
