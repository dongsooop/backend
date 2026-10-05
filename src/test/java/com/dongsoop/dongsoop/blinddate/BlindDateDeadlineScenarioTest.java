package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.support.BlindDateScenarioFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(15)
@DisplayName("과팅 선택 마감 추가 경계 시나리오")
class BlindDateDeadlineScenarioTest {
    @Test
    @DisplayName("C06 마감 예약을 두 번 실행해도 결과는 한 번")
    void repeatedDeadlineDoesNotDuplicateResults() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            flow.openChoices(session);
            Runnable deadline = flow.timers.remove();
            deadline.run();
            deadline.run();
            flow.queue.awaitIdle();
            assertThat(flow.recipients("/failed")).containsExactlyInAnyOrder(1L, 2L);
            assertThat(flow.results()).hasSize(2);
            assertThat(flow.sessions.getState(session)).isNull();
        }
    }

    @Test
    @DisplayName("C08 WAITING 세션의 선택 요청은 결과를 만들지 않음")
    void waitingSessionCannotMatch() {
        try (var flow = new BlindDateScenarioFixture(3)) {
            String session = flow.joinMembers(2);
            flow.queue.openChoices(session, 10_000);
            flow.choice.execute(session, 1L, 2L);
            flow.choice.execute(session, 2L, 1L);
            flow.queue.awaitIdle();
            assertThat(flow.createdPairs).isEmpty();
            assertThat(flow.results()).isEmpty();
            assertThat(flow.participants.getNotMatched(session)).containsExactlyInAnyOrder(1L, 2L);
        }
    }
}
