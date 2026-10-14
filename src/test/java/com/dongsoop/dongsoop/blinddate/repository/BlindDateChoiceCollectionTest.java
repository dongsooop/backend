package com.dongsoop.dongsoop.blinddate.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.blinddate.exception.InvalidBlindDateChoiceException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(15)
class BlindDateChoiceCollectionTest {
    private BlindDateParticipantStorageImpl storage(int count) {
        var storage = new BlindDateParticipantStorageImpl();
        for (long member = 1; member <= count; member++) {
            storage.addParticipant("session", member, "socket-" + member);
        }
        storage.openChoices("session");
        return storage;
    }

    @Test
    void missingAndExplicitNoChoiceAreDifferentAndFirstResponseIsImmutable() {
        var storage = storage(2);
        assertThat(storage.recordChoice("session", 1L, null)).isTrue();
        assertThat(storage.recordChoice("session", 1L, 2L)).isFalse();
        assertThat(storage.getChoices("session")).containsOnlyKeys(1L).containsEntry(1L, null);
        assertThat(storage.recordChoice("session", 2L, 1L)).isTrue();
        assertThat(storage.getChoices("session")).containsEntry(1L, null).containsEntry(2L, 1L);
        assertThat(storage.recordChoice("session", 2L, null)).isFalse();
        assertThat(storage.getNotMatched("session")).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void repeatedOpenDoesNotClearResponsesOrReleasePairClaim() {
        var storage = storage(2);
        storage.recordChoice("session", 1L, null);
        storage.openChoices("session");
        assertThat(storage.recordChoice("session", 2L, null)).isTrue();
        storage.openChoices("session");
        assertThat(storage.recordChoice("session", 2L, 1L)).isFalse();
        assertThat(storage.getChoices("session")).hasSize(2).containsEntry(1L, null);
    }

    @Test
    void repeatedOpenDoesNotReleaseClaimedMutualPair() {
        var storage = storage(3);
        storage.recordChoice("session", 1L, 2L);
        storage.recordChoice("session", 2L, 1L);
        assertThat(storage.claimMutualChoice("session", 1L)).isEqualTo(2L);
        storage.openChoices("session");
        assertThat(storage.claimMutualChoice("session", 1L)).isNull();
        assertThat(storage.claimMutualChoice("session", 2L)).isNull();
    }

    @Test
    void responseParticipantsAreFrozenAndDisconnectedMembersStillCount() {
        var storage = storage(2);
        storage.removeSocket("socket-2");
        storage.addParticipant("session", 3L, "socket-3");
        assertThatThrownBy(() -> storage.recordChoice("session", 3L, null))
                .isInstanceOf(InvalidBlindDateChoiceException.class);
        assertThatThrownBy(() -> storage.recordChoice("session", 1L, 3L))
                .isInstanceOf(InvalidBlindDateChoiceException.class);
        assertThat(storage.recordChoice("session", 1L, null)).isTrue();
        assertThat(storage.recordChoice("session", 2L, null)).isTrue();
        assertThat(storage.getChoices("session")).containsOnlyKeys(1L, 2L);
    }

    @Test
    void clearDropsResponsesAndPairClaims() {
        var storage = storage(2);
        storage.recordChoice("session", 1L, null);
        storage.recordChoice("session", 2L, null);
        storage.clear();
        assertThat(storage.getChoices("session")).isEmpty();
        assertThat(storage.recordChoice("session", 1L, null)).isFalse();
        storage.addParticipant("session", 1L, "new-one");
        storage.openChoices("session");
        assertThat(storage.recordChoice("session", 1L, null)).isTrue();
    }

    @Test
    void mutualPairCanBeClaimedOnlyOnceFromEitherMemberBeforeAllResponses() throws Exception {
        var storage = storage(3);
        storage.recordChoice("session", 1L, 2L);
        storage.recordChoice("session", 2L, 1L);
        var workers = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Future<Long> first = workers.submit(() -> {
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return storage.claimMutualChoice("session", 1L);
            });
            Future<Long> second = workers.submit(() -> {
                assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                return storage.claimMutualChoice("session", 2L);
            });
            start.countDown();
            Long firstClaim = first.get(5, TimeUnit.SECONDS);
            Long secondClaim = second.get(5, TimeUnit.SECONDS);
            assertThat((firstClaim != null) ^ (secondClaim != null)).isTrue();
            assertThat(storage.getChoices("session")).hasSize(2)
                    .containsEntry(1L, 2L).containsEntry(2L, 1L).doesNotContainKey(3L);
            assertThat(storage.claimMutualChoice("session", 1L)).isNull();
            assertThat(storage.claimMutualChoice("session", 2L)).isNull();
        } finally {
            workers.shutdownNow();
        }
    }

    @Test
    void simultaneousDuplicateResponsesStoreEachMemberOnlyOnce() throws Exception {
        var storage = storage(4);
        var workers = Executors.newFixedThreadPool(12);
        var start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 120; i++) {
                long member = i % 4 + 1;
                results.add(workers.submit(() -> {
                    assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                    return storage.recordChoice("session", member, null);
                }));
            }
            start.countDown();
            int claims = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) {
                    claims++;
                }
            }
            assertThat(claims).isEqualTo(4);
            assertThat(storage.getChoices("session")).hasSize(4);
        } finally {
            workers.shutdownNow();
        }
    }
}
