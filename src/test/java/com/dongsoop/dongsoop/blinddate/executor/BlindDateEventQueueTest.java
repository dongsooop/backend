package com.dongsoop.dongsoop.blinddate.executor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("과팅 세션 이벤트 큐")
class BlindDateEventQueueTest {

    private final BlindDateEventQueue queue = new BlindDateEventQueue(2);

    @AfterEach
    void tearDown() {
        queue.shutdown();
    }

    @Test
    @DisplayName("같은 세션의 선택과 마감은 제출 순서대로 하나씩 실행한다")
    void serializesChoicesAndCloseWithinSession() throws Exception {
        String sessionId = "session-a";
        queue.openChoices(sessionId, 10_000);
        List<String> order = new CopyOnWriteArrayList<>();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);

        queue.submitChoice(sessionId, () -> {
            order.add("choice-1");
            firstStarted.countDown();
            await(releaseFirst);
        });
        assertThat(firstStarted.await(3, TimeUnit.SECONDS)).isTrue();

        queue.submitChoice(sessionId, () -> {
            secondStarted.countDown();
            order.add("choice-2");
        });
        queue.closeChoices(sessionId, () -> order.add("close"));

        assertThat(secondStarted.await(200, TimeUnit.MILLISECONDS)).isFalse();
        releaseFirst.countDown();
        queue.awaitIdle();

        assertThat(order).containsExactly("choice-1", "choice-2", "close");
        assertThat(queue.activeSessionQueueCount()).isZero();
    }

    @Test
    @DisplayName("서로 다른 세션은 공용 worker에서 병렬 실행한다")
    void runsDifferentSessionsInParallel() throws Exception {
        queue.openChoices("session-a", 10_000);
        queue.openChoices("session-b", 10_000);
        CountDownLatch firstSessionStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstSession = new CountDownLatch(1);
        CountDownLatch secondSessionCompleted = new CountDownLatch(1);

        queue.submitChoice("session-a", () -> {
            firstSessionStarted.countDown();
            await(releaseFirstSession);
        });
        assertThat(firstSessionStarted.await(3, TimeUnit.SECONDS)).isTrue();

        queue.submitChoice("session-b", secondSessionCompleted::countDown);

        assertThat(secondSessionCompleted.await(3, TimeUnit.SECONDS)).isTrue();
        releaseFirstSession.countDown();
        queue.closeChoices("session-a", () -> { });
        queue.closeChoices("session-b", () -> { });
        queue.awaitIdle();
    }

    @Test
    @DisplayName("참여 큐가 막혀도 세션 선택 큐는 독립적으로 실행한다")
    void sessionQueueDoesNotWaitForParticipantQueue() throws Exception {
        CountDownLatch participantStarted = new CountDownLatch(1);
        CountDownLatch releaseParticipant = new CountDownLatch(1);
        CountDownLatch choiceCompleted = new CountDownLatch(1);
        queue.openChoices("session-a", 10_000);

        queue.submit(() -> {
            participantStarted.countDown();
            await(releaseParticipant);
        });
        assertThat(participantStarted.await(3, TimeUnit.SECONDS)).isTrue();

        queue.submitChoice("session-a", choiceCompleted::countDown);

        assertThat(choiceCompleted.await(3, TimeUnit.SECONDS)).isTrue();
        releaseParticipant.countDown();
        queue.closeChoices("session-a", () -> { });
        queue.awaitIdle();
    }

    @Test
    @DisplayName("동시에 실행하는 세션 수는 공용 worker 수를 넘지 않는다")
    void limitsParallelSessionsToWorkerCount() throws Exception {
        CountDownLatch twoWorkersStarted = new CountDownLatch(2);
        CountDownLatch releaseWorkers = new CountDownLatch(1);
        CountDownLatch thirdSessionStarted = new CountDownLatch(1);

        for (String sessionId : List.of("session-a", "session-b", "session-c")) {
            queue.openChoices(sessionId, 10_000);
        }
        queue.submitChoice("session-a", () -> {
            twoWorkersStarted.countDown();
            await(releaseWorkers);
        });
        queue.submitChoice("session-b", () -> {
            twoWorkersStarted.countDown();
            await(releaseWorkers);
        });
        assertThat(twoWorkersStarted.await(3, TimeUnit.SECONDS)).isTrue();

        queue.submitChoice("session-c", thirdSessionStarted::countDown);

        assertThat(thirdSessionStarted.await(200, TimeUnit.MILLISECONDS)).isFalse();
        releaseWorkers.countDown();
        assertThat(thirdSessionStarted.await(3, TimeUnit.SECONDS)).isTrue();
        for (String sessionId : List.of("session-a", "session-b", "session-c")) {
            queue.closeChoices(sessionId, () -> { });
        }
        queue.awaitIdle();
    }

    @Test
    @DisplayName("종료된 실행기에 작업을 제출해도 호출자에게 예외를 전파하지 않는다")
    void ignoresSubmissionsAfterShutdown() {
        queue.openChoices("session-a", 10_000);
        queue.shutdown();

        org.assertj.core.api.Assertions.assertThatCode(
                        () -> queue.submitChoice("session-a", () -> { }))
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(
                        () -> queue.closeChoices("session-a", () -> { }))
                .doesNotThrowAnyException();
        org.assertj.core.api.Assertions.assertThatCode(() -> queue.submit(() -> { }))
                .doesNotThrowAnyException();
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("테스트 작업 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
