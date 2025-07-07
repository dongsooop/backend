package com.dongsoop.dongsoop.report;

import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReportFilterTypeTest {

    @Test
    @DisplayName("ReportFilterType enum이 올바른 값들을 포함한다")
    void reportFilterType_ShouldContainExpectedValues() {
        // when & then
        assertThat(ReportFilterType.values()).containsExactly(
                ReportFilterType.ALL,
                ReportFilterType.UNPROCESSED,
                ReportFilterType.PROCESSED,
                ReportFilterType.ACTIVE_SANCTIONS
        );
    }

    @Test
    @DisplayName("ReportFilterType의 이름이 올바르다")
    void reportFilterType_ShouldHaveCorrectNames() {
        // when & then
        assertThat(ReportFilterType.ALL.name()).isEqualTo("ALL");
        assertThat(ReportFilterType.UNPROCESSED.name()).isEqualTo("UNPROCESSED");
        assertThat(ReportFilterType.PROCESSED.name()).isEqualTo("PROCESSED");
        assertThat(ReportFilterType.ACTIVE_SANCTIONS.name()).isEqualTo("ACTIVE_SANCTIONS");
    }

    @Test
    @DisplayName("문자열로부터 ReportFilterType을 올바르게 변환한다")
    void reportFilterType_ShouldConvertFromString() {
        // when & then
        assertThat(ReportFilterType.valueOf("ALL")).isEqualTo(ReportFilterType.ALL);
        assertThat(ReportFilterType.valueOf("UNPROCESSED")).isEqualTo(ReportFilterType.UNPROCESSED);
        assertThat(ReportFilterType.valueOf("PROCESSED")).isEqualTo(ReportFilterType.PROCESSED);
        assertThat(ReportFilterType.valueOf("ACTIVE_SANCTIONS")).isEqualTo(ReportFilterType.ACTIVE_SANCTIONS);
    }
}