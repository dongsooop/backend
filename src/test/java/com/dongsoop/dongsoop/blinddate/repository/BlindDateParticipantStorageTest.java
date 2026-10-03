package com.dongsoop.dongsoop.blinddate.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("과팅 참가자 저장소 단위 결과")
class BlindDateParticipantStorageTest {
    private final BlindDateParticipantStorageImpl storage = new BlindDateParticipantStorageImpl();

    @Test
    @DisplayName("R01 회원·소켓·세션 조회 일치와 세션 격리")
    void indexesAndSessionQueriesAgree() {
        storage.addParticipant("a", 1L, "one");
        storage.addParticipant("b", 2L, "two");
        assertThat(storage.getBySocketId("one").getMemberId()).isEqualTo(1L);
        assertThat(storage.getBySocketId("one").getSessionId()).isEqualTo("a");
        assertThat(storage.findAllBySessionId("a"))
                .extracting(p -> p.getMemberId())
                .containsExactly(1L);
        assertThat(storage.getParticipantsIdAndName("a")).containsOnlyKeys(1L);
        assertThat(storage.getByMemberId(99L)).isNull();
        assertThat(storage.getBySocketId("missing")).isNull();
    }

    @Test
    @DisplayName("R02 같은 소켓 재등록은 중복되지 않음")
    void repeatedSocketRegistrationIsIdempotent() {
        storage.addParticipant("a", 1L, "one");
        storage.addParticipant("a", 1L, "one");
        assertThat(storage.findAllBySessionId("a")).hasSize(1);
        assertThat(storage.getByMemberId(1L).getSocketIds()).containsExactly("one");
    }

    @Test
    @DisplayName("R03 다른 세션 중복 입장 거부에도 기존 인덱스 유지")
    void rejectedEntryDoesNotDestroyExistingIndexes() {
        storage.addParticipant("a", 1L, "one");
        assertThatThrownBy(() -> storage.addParticipant("b", 1L, "two"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(storage.getByMemberId(1L).getSessionId()).isEqualTo("a");
        assertThat(storage.getBySocketId("one").getMemberId()).isEqualTo(1L);
        assertThat(storage.getBySocketId("two")).isNull();
        assertThat(storage.findAllBySessionId("b")).isEmpty();
    }

    @Test
    @DisplayName("R04 소켓 제거 반환값과 남은 연결·회원 기록")
    void socketRemovalPreservesParticipantUntilCallerRemovesIt() {
        storage.addParticipant("a", 1L, "one");
        storage.addParticipant("a", 1L, "two");
        assertThat(storage.removeSocket("one")).isFalse();
        assertThat(storage.getBySocketId("one")).isNull();
        assertThat(storage.getByMemberId(1L).getSocketIds()).containsExactly("two");
        assertThat(storage.removeSocket("two")).isTrue();
        assertThat(storage.getByMemberId(1L)).isNotNull();
        assertThat(storage.getByMemberId(1L).hasNoSockets()).isTrue();
        assertThatThrownBy(() -> storage.removeSocket("two"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("R05 참가자 제거는 모든 소켓 제거, 다른 회원 유지")
    void participantRemovalClearsOnlyTheirIndexes() {
        storage.addParticipant("a", 1L, "one");
        storage.addParticipant("a", 1L, "two");
        storage.addParticipant("a", 2L, "other");
        storage.removeParticipant(1L);
        storage.removeParticipant(1L);
        assertThat(storage.getByMemberId(1L)).isNull();
        assertThat(storage.getBySocketId("one")).isNull();
        assertThat(storage.getBySocketId("two")).isNull();
        assertThat(storage.getBySocketId("other").getMemberId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("R06 초기화 후 인덱스·선택·매칭 제거와 익명 번호 재시작")
    void clearResetsAllPublicState() {
        storage.addParticipant("a", 1L, "one");
        storage.addParticipant("a", 2L, "two");
        storage.recordChoice("a", 1L, 2L);
        storage.recordChoice("a", 2L, 1L);
        storage.clear();
        assertThat(storage.getByMemberId(1L)).isNull();
        assertThat(storage.getBySocketId("one")).isNull();
        assertThat(storage.isMatched("a", 1L)).isFalse();
        assertThat(storage.findAllBySessionId("a")).isEmpty();
        assertThat(storage.addParticipant("a", 1L, "new").getAnonymousName()).isEqualTo("익명1");
        storage.addParticipant("a", 2L, "new2");
        assertThat(storage.recordChoice("a", 1L, 2L)).isFalse();
        assertThat(storage.recordChoice("a", 2L, 1L)).isTrue();
    }

    @Test
    @DisplayName("R07 최초 선택 유지, 다른 세션 선택은 별개")
    void firstChoiceAndSessionIsolation() {
        assertThat(storage.recordChoice("a", 1L, 2L)).isFalse();
        assertThat(storage.recordChoice("a", 1L, 3L)).isFalse();
        assertThat(storage.recordChoice("a", 3L, 1L)).isFalse();
        assertThat(storage.recordChoice("b", 2L, 1L)).isFalse();
        assertThat(storage.recordChoice("a", 2L, 1L)).isTrue();
        assertThat(storage.isMatched("b", 1L)).isFalse();
    }

    @Test
    @DisplayName("R08 상호 선택 회원만 미매칭 목록에서 제외")
    void matchedPairIsExcludedFromUnmatchedMembers() {
        for (long member = 1; member <= 3; member++)
            storage.addParticipant("a", member, "s" + member);
        storage.recordChoice("a", 1L, 2L);
        storage.recordChoice("a", 2L, 1L);
        assertThat(storage.isMatched("a", 1L)).isTrue();
        assertThat(storage.isMatched("a", 2L)).isTrue();
        assertThat(storage.getNotMatched("a")).containsExactly(3L);
        assertThat(storage.getNotMatched("missing")).isEmpty();
    }
}
