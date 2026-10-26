package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.blinddate.support.BlindDateScenarioFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
@DisplayName("과팅 운영 종료와 참가 기록 초기화")
class BlindDateLifecycleScenarioTest {
    @Test
    @DisplayName("F03 운영 종료는 입장 차단, 유예 이후 모든 저장소 초기화")
    void scheduledOperatingEndAndGracePeriodCleanup() {
        try (var flow = new BlindDateScenarioFixture(3)) {
            String session = flow.joinMembers(2);
            flow.endOperation();
            assertThat(flow.service.isAvailable()).isFalse();
            assertThatThrownBy(() -> flow.join(3, "three"))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(flow.participants.findAllBySessionId(session)).hasSize(2);
            flow.runNextTimer();
            flow.queue.awaitIdle();
            assertThat(flow.operation.getPointer()).isNull();
            assertThat(flow.operation.getMaxSessionMemberCount()).isNull();
            assertThat(flow.sessions.getState(session)).isNull();
            assertThat(flow.participants.getByMemberId(1L)).isNull();
            assertThat(flow.participants.getBySocketId("socket-1")).isNull();
        }
    }

    @Test
    @DisplayName("F04 기록 초기화 이후 운영 정원 유지와 새 입장")
    void participantResetAllowsNewSessionWithoutClosingOperation() {
        try (var flow = new BlindDateScenarioFixture(3)) {
            String old = flow.joinMembers(2);
            flow.service.resetParticipants();
            flow.queue.awaitIdle();
            assertThat(flow.service.isAvailable()).isTrue();
            assertThat(flow.operation.getMaxSessionMemberCount()).isEqualTo(3);
            assertThat(flow.sessions.getState(old)).isNull();
            assertThat(flow.participants.getBySocketId("socket-1")).isNull();
            var newSession = flow.join(1, "new").get("sessionId");
            assertThat(newSession).isNotNull().isNotEqualTo(old);
            assertThat(flow.participants.getByMemberId(1L).getAnonymousName()).isEqualTo("익명1");
        }
    }
}
