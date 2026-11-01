package com.dongsoop.dongsoop.blinddate.executor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(20)
class BlindDateLifecycleConcurrencyTest {
  @ParameterizedTest(name = "worker={0}: 선택·마감 뒤 중첩 초기화")
  @ValueSource(ints = {1, 2, 5})
  void overlappingResetsDrainChoicesAndKeepAdmissionClosed(int workers) throws Exception {
    var queue = new BlindDateEventQueue(workers);
    var events = new CopyOnWriteArrayList<String>();
    var choiceStarted = new CountDownLatch(1);
    var releaseChoice = new CountDownLatch(1);
    var secondCleanupStarted = new CountDownLatch(1);
    var releaseSecondCleanup = new CountDownLatch(1);
    try {
      queue.openChoices("old");
      queue.submitChoice(
          "old",
          () -> {
            choiceStarted.countDown();
            await(releaseChoice);
            events.add("choice");
          });
      assertThat(choiceStarted.await(5, TimeUnit.SECONDS)).isTrue();
      queue.submitChoice("old", () -> events.add("queued-choice"));
      queue.closeChoices("old", () -> events.add("timeout"));
      queue.submitCleanup(() -> events.add("reset-1"));
      queue.submitCleanup(
          () -> {
            secondCleanupStarted.countDown();
            await(releaseSecondCleanup);
            events.add("reset-2");
          });
      assertThat(queue.openChoices("during", () -> true)).isFalse();
      queue.submitChoice("old", () -> events.add("late-choice"));
      assertThat(events).isEmpty();
      releaseChoice.countDown();
      assertThat(secondCleanupStarted.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(events).containsExactly("choice", "queued-choice", "timeout", "reset-1");
      assertThat(queue.openChoices("between", () -> true)).isFalse();
      releaseSecondCleanup.countDown();
      queue.awaitIdle();
      assertThat(queue.openChoices("old", () -> false)).isFalse();
      assertThat(queue.openChoices("fresh", () -> true)).isTrue();
      queue.submitChoice("fresh", () -> events.add("fresh-choice"));
      queue.awaitIdle();
      assertThat(events)
          .containsExactly(
              "choice", "queued-choice", "timeout", "reset-1", "reset-2", "fresh-choice");
    } finally {
      releaseChoice.countDown();
      releaseSecondCleanup.countDown();
      queue.shutdown();
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("latch timeout");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(exception);
    }
  }
}
