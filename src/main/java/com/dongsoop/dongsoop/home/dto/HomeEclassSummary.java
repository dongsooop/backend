package com.dongsoop.dongsoop.home.dto;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 홈의 "과제" 탭에 표시할 요약. 목록은 과제 화면에서 보여주므로 여기서는 개수와 가장 임박한 한 건만 담는다.
 */
public record HomeEclassSummary(

        boolean linked,
        long upcomingCount,
        String nearestCourseName,
        String nearestTitle,
        LocalDateTime nearestDueAt,
        Long nearestDDay
) {

    public static HomeEclassSummary unlinked() {
        return new HomeEclassSummary(false, 0L, null, null, null, null);
    }

    public static HomeEclassSummary empty() {
        return new HomeEclassSummary(true, 0L, null, null, null, null);
    }

    public static HomeEclassSummary of(long upcomingCount, EclassAssignment nearest, LocalDate today) {
        if (nearest == null) {
            return empty();
        }

        long dDay = ChronoUnit.DAYS.between(today, nearest.getDueAt().toLocalDate());

        return new HomeEclassSummary(true, upcomingCount, nearest.getCourseName(), nearest.getTitle(),
                nearest.getDueAt(), dDay);
    }
}
