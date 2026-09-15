package com.dongsoop.dongsoop.home.dto;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 홈의 "과제" 탭에 표시할 요약. 전체 목록은 과제 화면에서 보여주므로 여기서는 개수와 임박한 몇 건만 담는다.
 * {@code upcoming}과 {@code upcomingCount}는 같은 기준(미제출·미삭제·마감 전)으로 센다.
 *
 * <p>{@code nearest*}는 목록이 생기기 전 배포된 앱이 읽고 있어 그대로 둔다. 값은 {@code upcoming}의 첫 항목과 같다.
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
        Long nearestDDay,
        List<HomeEclassAssignment> upcoming
) {

    public static HomeEclassSummary unlinked() {
        return new HomeEclassSummary(false, null, 0L, null, null, null, null, List.of());
    }

    public static HomeEclassSummary expired() {
        return new HomeEclassSummary(true, EclassLinkStatus.EXPIRED, 0L, null, null, null, null, List.of());
    }

    public static HomeEclassSummary of(long upcomingCount, List<EclassAssignment> upcoming, LocalDate today) {
        List<HomeEclassAssignment> items = upcoming.stream()
                .map(assignment -> HomeEclassAssignment.from(assignment, today))
                .toList();
        if (items.isEmpty()) {
            return new HomeEclassSummary(true, EclassLinkStatus.ACTIVE, 0L, null, null, null, null, items);
        }

        HomeEclassAssignment nearest = items.get(0);

        return new HomeEclassSummary(true, EclassLinkStatus.ACTIVE, upcomingCount, nearest.courseName(),
                nearest.title(), nearest.dueAt(), nearest.dDay(), items);
    }
}
