package com.dongsoop.dongsoop.eclass;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.home.dto.HomeEclassAssignment;
import com.dongsoop.dongsoop.home.dto.HomeEclassSummary;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HomeEclassSummaryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 1);

    private final EclassLink link = new EclassLink(MemberDevice.builder().id(1L).build(), "테스트", "encrypted");

    private EclassAssignment assignment(long assignId, LocalDateTime dueAt) {
        return new EclassAssignment(link, assignId, 9000L + assignId, "자료구조", "과제 " + assignId, dueAt, dueAt);
    }

    @Test
    @DisplayName("목록은 받은 순서대로 담고, nearest 필드는 첫 항목과 같다")
    void upcomingListAndNearestAgree() {
        List<EclassAssignment> upcoming = List.of(
                assignment(1L, TODAY.plusDays(2).atTime(23, 55)),
                assignment(2L, TODAY.plusDays(5).atTime(23, 55)),
                assignment(3L, TODAY.plusDays(9).atTime(23, 55)));

        HomeEclassSummary summary = HomeEclassSummary.of(7L, upcoming, TODAY);

        assertThat(summary.upcomingCount()).isEqualTo(7L);
        assertThat(summary.upcoming()).extracting(HomeEclassAssignment::dDay).containsExactly(2L, 5L, 9L);
        assertThat(summary.nearestTitle()).isEqualTo("과제 1");
        assertThat(summary.nearestDDay()).isEqualTo(2L);
        assertThat(summary.upcoming().get(0).submitted()).isFalse();
    }

    @Test
    @DisplayName("미연동·만료·과제 없음은 모두 빈 배열이다")
    void emptyListForNonActiveStates() {
        assertThat(HomeEclassSummary.unlinked().upcoming()).isEmpty();
        assertThat(HomeEclassSummary.expired().upcoming()).isEmpty();
        assertThat(HomeEclassSummary.of(0L, List.of(), TODAY).upcoming()).isEmpty();
        assertThat(HomeEclassSummary.of(0L, List.of(), TODAY).nearestTitle()).isNull();
    }
}
