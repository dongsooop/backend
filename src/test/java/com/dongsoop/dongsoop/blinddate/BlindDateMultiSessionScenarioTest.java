package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.support.BlindDateScenarioFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(15)
@DisplayName("과팅 여러 세션의 결과 격리")
class BlindDateMultiSessionScenarioTest {
    @Test
    @DisplayName("C07 한 세션 종료 후 다른 세션의 상호 선택과 성공 유지")
    void closingFirstSessionDoesNotRejectSecondSessionChoices() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String first = (String) flow.join(1, "one").get("sessionId");
            flow.join(2, "two");
            String second = (String) flow.join(3, "three").get("sessionId");
            flow.join(4, "four");
            assertThat(first).isNotEqualTo(second);
            flow.openChoices(first);
            Runnable firstDeadline = flow.timers.remove();
            flow.openChoices(second);
            firstDeadline.run();
            flow.queue.awaitIdle();
            assertThat(flow.sessions.getState(first)).isNull();
            assertThat(flow.sessions.getState(second)).isNotNull();
            flow.choice.execute(second, 3L, 4L);
            flow.choice.execute(second, 4L, 3L);
            flow.finishChoices();
            assertThat(flow.recipients("/chatroom")).containsExactlyInAnyOrder(3L, 4L);
            assertThat(flow.recipients("/failed")).containsExactlyInAnyOrder(1L, 2L);
            assertThat(flow.results())
                    .allSatisfy(
                            event -> {
                                if (event.destination().endsWith("/chatroom"))
                                    assertThat(event.destination()).contains(second);
                                else assertThat(event.destination()).contains(first);
                            });
        }
    }
}
