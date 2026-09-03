package com.dongsoop.dongsoop.home.dto;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 홈의 "과제" 탭에 표시할 요약. 목록은 과제 화면에서 보여주므로 여기서는 개수와 가장 임박한 한 건만 담는다.
 *
 * <p>연동이 끊긴 상태({@code status = EXPIRED})에서는 과제를 가져올 수 없어 개수가 0이 된다.
 * 이때 "과제 없음"으로 보여주면 마감이 없다고 잘못 안심시키므로, 상태를 함께 내려
 * 앱이 재연동을 안내할 수 있게 한다.
 */
public record HomeEclassSummary(

        boolean linked,
        EclassLinkStatus status,
        long upcomingCount,
        String nearestCourseName,
        String nearestTitle,
        LocalDateTime nearestDueAt,
        Long nearestDDay
) {

    public static HomeEclassSummary unlinked() {
        return new HomeEclassSummary(false, null, 0L, null, null, null, null);
    }

    public static HomeEclassSummary expired() {
        return new HomeEclassSummary(true, EclassLinkStatus.EXPIRED, 0L, null, null, null, null);
    }

    public static HomeEclassSummary of(long upcomingCount, EclassAssignment nearest, LocalDate today) {
        if (nearest == null) {
            return new HomeEclassSummary(true, EclassLinkStatus.ACTIVE, 0L, null, null, null, null);
        }

        long dDay = ChronoUnit.DAYS.between(today, nearest.getDueAt().toLocalDate());

        return new HomeEclassSummary(true, EclassLinkStatus.ACTIVE, upcomingCount, nearest.getCourseName(),
                nearest.getTitle(), nearest.getDueAt(), dDay);
    }
}
