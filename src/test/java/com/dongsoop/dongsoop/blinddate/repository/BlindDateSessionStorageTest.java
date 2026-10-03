package com.dongsoop.dongsoop.blinddate.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("과팅 세션 저장소 단위 결과")
class BlindDateSessionStorageTest {
    private final BlindDateSessionStorageImpl storage = new BlindDateSessionStorageImpl();

    @Test
    @DisplayName("R09 생성·시작·종료 상태 전환")
    void lifecycle() {
        String id = storage.create().getSessionId();
        assertThat(storage.getState(id)).isEqualTo(SessionState.WAITING);
        assertThat(storage.isWaiting(id)).isTrue();
        assertThat(storage.isProcessing(id)).isFalse();
        storage.start(id);
        assertThat(storage.getState(id)).isEqualTo(SessionState.PROCESSING);
        assertThat(storage.isWaiting(id)).isFalse();
        storage.terminate(id);
        assertThat(storage.getState(id)).isNull();
        assertThat(storage.isProcessing(id)).isFalse();
    }

    @Test
    @DisplayName("R10 없는 세션·반복 종료는 다른 세션에 영향 없음")
    void unknownAndRepeatedOperationsAreHarmless() {
        String first = storage.create().getSessionId();
        String second = storage.create().getSessionId();
        assertThat(first).isNotEqualTo(second);
        storage.start("missing");
        storage.terminate("missing");
        storage.terminate(first);
        storage.terminate(first);
        assertThat(storage.isWaiting(second)).isTrue();
        assertThat(storage.isWaiting("missing")).isFalse();
    }

    @Test
    @DisplayName("R11 전체 초기화는 대기·진행 세션 모두 제거")
    void clearAllStates() {
        String first = storage.create().getSessionId();
        String second = storage.create().getSessionId();
        storage.start(second);
        storage.clear();
        assertThat(storage.getState(first)).isNull();
        assertThat(storage.getState(second)).isNull();
    }
}
