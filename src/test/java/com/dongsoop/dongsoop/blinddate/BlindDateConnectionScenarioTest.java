package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import com.dongsoop.dongsoop.blinddate.support.BlindDateScenarioFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(10)
@DisplayName("과팅 연결·연결 해제 결과 시나리오")
class BlindDateConnectionScenarioTest {
    @Test
    @DisplayName("L05 다른 소켓을 유지한 채 한 소켓 연결 해제")
    void oneSocketLeavesOtherConnectionActive() {
        try (var flow = new BlindDateScenarioFixture(3)) {
            String session = (String) flow.join(1, "one").get("sessionId");
            String name = flow.participants.getAnonymousName(1L);
            flow.join(1, "two");
            flow.disconnect.execute("one", 1L, session);
            flow.queue.awaitIdle();
            assertThat(flow.participants.getBySocketId("one")).isNull();
            assertThat(flow.participants.getBySocketId("two").getMemberId()).isEqualTo(1L);
            assertThat(flow.participants.getByMemberId(1L).getSocketIds()).containsExactly("two");
            assertThat(flow.participants.getAnonymousName(1L)).isEqualTo(name);
        }
    }

    @Test
    @DisplayName("L06 마지막 대기 소켓 제거 후 새 참가자 입장")
    void waitingMemberLeavesAndAnotherCanJoin() {
        try (var flow = new BlindDateScenarioFixture(3)) {
            String session = (String) flow.join(1, "one").get("sessionId");
            flow.disconnect.execute("one", 1L, session);
            flow.queue.awaitIdle();
            assertThat(flow.participants.getByMemberId(1L)).isNull();
            assertThat(flow.participants.getBySocketId("one")).isNull();
            assertThat(flow.join(2, "two").get("sessionId")).isEqualTo(session);
            assertThat(flow.participants.findAllBySessionId(session)).hasSize(1);
        }
    }

    @Test
    @DisplayName("L07 진행 중 모든 소켓 해제 후 같은 세션·익명 이름으로 복귀")
    void processingMemberCanReconnect() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            String name = flow.participants.getAnonymousName(1L);
            flow.disconnect.execute("socket-1", 1L, session);
            flow.queue.awaitIdle();
            assertThat(flow.participants.getByMemberId(1L)).isNotNull();
            assertThat(flow.join(1, "new").get("sessionId")).isEqualTo(session);
            assertThat(flow.participants.getAnonymousName(1L)).isEqualTo(name);
            assertThat(flow.sessions.getState(session)).isEqualTo(SessionState.PROCESSING);
            assertThat(flow.participants.getByMemberId(1L).getSocketIds()).containsExactly("new");
        }
    }

    @Test
    @DisplayName("L08 종료된 세션 재접속은 새 입장 없음")
    void terminatedSessionCannotBeRejoined() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            flow.sessions.terminate(session);
            assertThat(flow.join(1, "new")).doesNotContainKey("sessionId");
            assertThat(flow.participants.getBySocketId("new")).isNull();
            assertThat(flow.participants.getByMemberId(1L).getSessionId()).isEqualTo(session);
        }
    }

    @Test
    @DisplayName("L09 중복·없는 소켓 퇴장은 다른 참가자에 영향 없음")
    void duplicateDisconnectIsHarmless() {
        try (var flow = new BlindDateScenarioFixture(3)) {
            String session = flow.joinMembers(2);
            flow.disconnect.execute("socket-1", 1L, session);
            flow.disconnect.execute("socket-1", 1L, session);
            flow.disconnect.execute("missing", 99L, session);
            flow.queue.awaitIdle();
            assertThat(flow.participants.findAllBySessionId(session))
                    .extracting(p -> p.getMemberId())
                    .containsExactly(2L);
            assertThat(flow.participants.getBySocketId("socket-2").getMemberId()).isEqualTo(2L);
        }
    }

    @Test
    @DisplayName("L10 입장 이벤트 실패는 신규 등록 롤백")
    void joinDeliveryFailureRollsBackRegistration() {
        try (var flow = new BlindDateScenarioFixture(3)) {
            doThrow(new IllegalStateException("delivery failed"))
                    .when(flow.messaging)
                    .convertAndSendToUser(eq("1"), anyString(), any(Object.class));
            var attributes = flow.join(1, "one");
            flow.queue.awaitIdle();
            assertThat(attributes).doesNotContainKey("sessionId");
            assertThat(flow.participants.getByMemberId(1L)).isNull();
            assertThat(flow.participants.getBySocketId("one")).isNull();
        }
    }

    @Test
    @DisplayName("L11 종료 후 연결 해제는 재입장 방지 기록 보존")
    void disconnectAfterTerminationPreservesRecord() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            flow.sessions.terminate(session);
            flow.disconnect.execute("socket-1", 1L, session);
            flow.queue.awaitIdle();
            assertThat(flow.participants.getByMemberId(1L).getSessionId()).isEqualTo(session);
            assertThat(flow.join(1, "new")).doesNotContainKey("sessionId");
        }
    }
}
