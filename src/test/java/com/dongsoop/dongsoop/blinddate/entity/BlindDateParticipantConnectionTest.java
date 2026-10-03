package com.dongsoop.dongsoop.blinddate.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("과팅 참가자 연결 정보 단위 결과")
class BlindDateParticipantConnectionTest {
    @Test
    @DisplayName("E01 같은 소켓 추가는 중복 없이 유지")
    void addingSameSocketIsIdempotent() {
        var member = ParticipantInfo.create("session", 1L, "one", "익명1");
        member.addSocket("one");
        member.addSocket("two");
        assertThat(member.getSocketIds()).containsExactlyInAnyOrder("one", "two");
    }

    @Test
    @DisplayName("E02 마지막 소켓 제거와 없는 소켓 제거 결과")
    void lastConnectionRemoval() {
        var member = ParticipantInfo.create("session", 1L, "one", "익명1");
        assertThat(member.removeSocket("missing")).isFalse();
        assertThat(member.hasNoSockets()).isFalse();
        assertThat(member.removeSocket("one")).isTrue();
        assertThat(member.hasNoSockets()).isTrue();
    }

    @Test
    @DisplayName("E03 조회한 소켓 목록을 외부에서 수정할 수 없음")
    void socketViewCannotBeMutated() {
        var member = ParticipantInfo.create("session", 1L, "one", "익명1");
        assertThatThrownBy(() -> member.getSocketIds().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(member.getSocketIds()).containsExactly("one");
    }
}
