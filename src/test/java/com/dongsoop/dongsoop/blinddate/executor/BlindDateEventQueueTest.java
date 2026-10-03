package com.dongsoop.dongsoop.blinddate.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Timeout(5)
@DisplayName("과팅 이벤트 큐 단위 결과")
class BlindDateEventQueueTest {
    private final BlindDateEventQueue queue = new BlindDateEventQueue();
    private final List<Integer> processed = new CopyOnWriteArrayList<>();

    @AfterEach
    void close() {
        queue.shutdown();
    }

    @Test
    @DisplayName("Q01 접수된 이벤트 순서 유지와 완료 대기")
    void eventsAreProcessedInSubmissionOrder() {
        for (int index = 0; index < 100; index++) {
            int value = index;
            queue.submit(() -> processed.add(value));
        }
        queue.awaitIdle();
        assertThat(processed)
                .containsExactlyElementsOf(
                        java.util.stream.IntStream.range(0, 100).boxed().toList());
    }

    @Test
    @DisplayName("Q02 작업 예외가 다음 이벤트를 막지 않음")
    void eventFailureDoesNotStopQueue() {
        queue.submit(
                () -> {
                    throw new IllegalStateException("failed");
                });
        queue.submit(() -> processed.add(1));
        queue.awaitIdle();
        assertThat(processed).containsExactly(1);
    }

    @Test
    @DisplayName("Q03 큐 종료 후 작업 제출은 예외 전파나 실행 없음")
    void stoppedQueueRejectsWithoutThrowing() {
        queue.shutdown();
        assertThatCode(() -> queue.submit(() -> processed.add(1))).doesNotThrowAnyException();
        assertThat(processed).isEmpty();
    }

    @Test
    @DisplayName("중복 마감은 최종 작업을 한 번만 실행")
    void duplicateCloseIsIdempotent() {
        queue.openChoices("session", 10_000);
        queue.submitChoice("session", () -> processed.add(1));
        queue.closeChoices("session", () -> processed.add(2));
        queue.closeChoices("session", () -> processed.add(3));
        queue.submitChoice("session", () -> processed.add(4));
        queue.awaitIdle();
        assertThat(processed).containsExactly(1, 2);
    }
}
