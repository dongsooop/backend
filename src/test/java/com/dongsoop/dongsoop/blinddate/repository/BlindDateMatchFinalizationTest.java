package com.dongsoop.dongsoop.blinddate.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class BlindDateMatchFinalizationTest {

    @Test
    void successfulPairIsNeverIncludedInFailureResults() {
        var storage = participants();
        storage.recordChoice("session", 1L, 2L);
        assertThat(storage.recordChoice("session", 2L, 1L)).isTrue();

        assertThat(storage.finalizeChoices("session")).containsExactly(3L);
        assertThat(storage.finalizeChoices("session")).isEmpty();
    }

    @Test
    void failedResultCannotBeReplacedWithSuccess() {
        var storage = participants();
        storage.recordChoice("session", 1L, 2L);

        assertThat(storage.finalizeChoices("session")).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(storage.recordChoice("session", 2L, 1L)).isFalse();
        assertThat(storage.isMatched("session", 1L)).isFalse();
        assertThat(storage.isMatched("session", 2L)).isFalse();
    }

    @Test
    void concurrentMatchingAndFinalizationProduceExclusiveResults() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            for (int attempt = 0; attempt < 100; attempt++) {
                var storage = participants();
                storage.recordChoice("session", 1L, 2L);
                var start = new CountDownLatch(1);
                var matched = executor.submit(() -> {
                    start.await();
                    return storage.recordChoice("session", 2L, 1L);
                });
                var failed = executor.submit(() -> {
                    start.await();
                    return storage.finalizeChoices("session");
                });
                start.countDown();

                boolean success = matched.get(5, TimeUnit.SECONDS);
                Set<Long> failures = failed.get(5, TimeUnit.SECONDS);
                assertThat(failures).contains(3L);
                assertThat(failures.contains(1L)).isEqualTo(!success);
                assertThat(failures.contains(2L)).isEqualTo(!success);
                assertThat(storage.isMatched("session", 1L)).isEqualTo(success);
                assertThat(storage.isMatched("session", 2L)).isEqualTo(success);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private BlindDateParticipantStorageImpl participants() {
        var storage = new BlindDateParticipantStorageImpl();
        for (long id = 1; id <= 3; id++) {
            storage.addParticipant("session", id, "socket-" + id);
        }
        return storage;
    }
}
