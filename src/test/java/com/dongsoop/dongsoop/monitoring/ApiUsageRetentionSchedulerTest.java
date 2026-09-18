package com.dongsoop.dongsoop.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.monitoring.scheduler.ApiUsageRetentionScheduler;
import java.time.YearMonth;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiUsageRetentionSchedulerTest {

    @Test
    @DisplayName("보존 달수보다 오래된 월 인덱스만 고르고, 형식이 다른 이름은 건드리지 않는다")
    void picksOnlyExpiredMonthlyIndices() {
        Set<String> indices = Set.of(
                "api-usage-2026.05", "api-usage-2026.06", "api-usage-2026.09",
                "api-usage-bad", "boards");

        assertThat(ApiUsageRetentionScheduler.expiredOf(indices, YearMonth.of(2026, 9), 3))
                .containsExactly("api-usage-2026.05");
    }
}
