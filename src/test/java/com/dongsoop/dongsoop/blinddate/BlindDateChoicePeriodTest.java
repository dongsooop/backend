package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("과팅 선택 접수 기간")
class BlindDateChoicePeriodTest {

    private final BlindDateEventQueue queue = new BlindDateEventQueue();
    private final List<String> results = new CopyOnWriteArrayList<>();

    @AfterEach
    void tearDown() {
        queue.shutdown();
    }

    @Test
    @DisplayName("선택 단계 시작 전 요청은 접수하지 않는다")
    void choicesBeforePeriodAreIgnored() {
        queue.submitChoice("session", () -> results.add("choice"));
        queue.awaitIdle();
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("접수는 명시적으로 닫힐 때까지 유지된다")
    void choicesRemainOpenUntilExplicitlyClosed() {
        queue.openChoices("session");
        queue.submitChoice("session", () -> results.add("choice"));
        queue.closeChoices("session", () -> results.add("finalized"));
        queue.awaitIdle();
        assertThat(results).containsExactly("choice", "finalized");
    }

    @Test
    @DisplayName("한 세션의 마감은 다른 세션의 선택 접수를 막지 않는다")
    void closingOneSessionDoesNotCloseAnother() {
        queue.openChoices("first");
        queue.openChoices("second");
        queue.closeChoices("first", () -> results.add("first-finalized"));
        queue.submitChoice("first", () -> results.add("first-choice"));
        queue.submitChoice("second", () -> results.add("second-choice"));
        queue.closeChoices("first", () -> results.add("duplicate-finalized"));
        queue.awaitIdle();
        assertThat(results).containsExactlyInAnyOrder("first-finalized", "second-choice");
    }
}
