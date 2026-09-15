package com.dongsoop.dongsoop.home.dto;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 홈 과제 카드의 목록 항목. 상세 정보는 과제 화면에서 보여주므로 카드에 그릴 것만 담는다.
 */
public record HomeEclassAssignment(

        String courseName,
        String title,
        LocalDateTime dueAt,
        long dDay,
        boolean submitted
) {

    public static HomeEclassAssignment from(EclassAssignment assignment, LocalDate today) {
        long dDay = ChronoUnit.DAYS.between(today, assignment.getDueAt().toLocalDate());

        return new HomeEclassAssignment(assignment.getCourseName(), assignment.getTitle(), assignment.getDueAt(),
                dDay, assignment.isSubmitted());
    }
}
