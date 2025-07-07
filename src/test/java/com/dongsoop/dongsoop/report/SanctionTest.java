package com.dongsoop.dongsoop.report;

import com.dongsoop.dongsoop.report.entity.Sanction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SanctionTest {

    @Test
    @DisplayName("제재 종료일이 현재 시간보다 이전이면 만료된 것으로 판단한다")
    void isExpired_WhenEndDateIsPast_ShouldReturnTrue() {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pastEndDate = now.minusDays(1);

        Sanction sanction = Sanction.builder()
                .endDate(pastEndDate)
                .build();

        // when
        boolean expired = sanction.isCurrentlyExpired();

        // then
        assertThat(expired).isTrue();
    }

    @Test
    @DisplayName("제재 종료일이 현재 시간보다 이후이면 만료되지 않은 것으로 판단한다")
    void isExpired_WhenEndDateIsFuture_ShouldReturnFalse() {
        // given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureEndDate = now.plusDays(1);

        Sanction sanction = Sanction.builder()
                .endDate(futureEndDate)
                .build();

        // when
        boolean expired = sanction.isCurrentlyExpired();

        // then
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("제재를 비활성화하면 isActive가 false로 변경된다")
    void deactivate_ShouldSetIsActiveToFalse() {
        // given
        Sanction sanction = Sanction.builder()
                .isActive(true)
                .build();

        // when
        sanction.deactivate();

        // then
        assertThat(sanction.getIsActive()).isFalse();
    }
}