package com.dongsoop.dongsoop.blinddate.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SessionInfo Entity 단위 테스트
 */
@DisplayName("SessionInfo Entity 단위 테스트")
class SessionInfoTest {

    @Test
    @DisplayName("create - 초기 상태 WAITING")
    void create_InitialStateIsWaiting() {
        // when
        SessionInfo session = SessionInfo.create();

        // then
        assertThat(session.getState()).isEqualTo(SessionState.WAITING);
        // assertThat(session.getParticipantCount()).isEqualTo(0); // Removed as participant count is managed elsewhere
        assertThat(session.getSessionId()).isNotNull();
        assertThat(session.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("start - PROCESSING 상태로 변경")
    void start_ChangesToProcessing() {
        // given
        SessionInfo session = SessionInfo.create();

        // when
        session.start();

        // then
        assertThat(session.getState()).isEqualTo(SessionState.PROCESSING);
    }

    @Test
    @DisplayName("terminate - ENDED 상태로 변경")
    void terminate_ChangesToEnded() {
        // given
        SessionInfo session = SessionInfo.create();

        // when
        session.terminate();

        // then
        assertThat(session.getState()).isEqualTo(SessionState.ENDED);
    }
}