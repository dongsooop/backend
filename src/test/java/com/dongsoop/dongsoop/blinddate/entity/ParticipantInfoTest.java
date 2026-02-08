package com.dongsoop.dongsoop.blinddate.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * ParticipantInfo Entity 단위 테스트
 */
@DisplayName("ParticipantInfo Entity 단위 테스트")
class ParticipantInfoTest {

    @Test
    @DisplayName("create - 모든 필드 설정")
    void create_SetsAllFields() {
        // when
        ParticipantInfo participant = ParticipantInfo.create(
                "session-123",
                1L,
                "socket-456",
                "익명1"
        );

        // then
        assertThat(participant.getSessionId()).isEqualTo("session-123");
        assertThat(participant.getMemberId()).isEqualTo(1L);
        assertThat(participant.getSocketIds()).containsExactly("socket-456"); // Modified
        assertThat(participant.getAnonymousName()).isEqualTo("익명1");
        assertThat(participant.getJoinedAt()).isNotNull();
    }

    @Test
    @DisplayName("joinedAt - 현재 시간으로 설정")
    void create_SetsJoinedAtToNow() {
        // given
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        // when
        ParticipantInfo participant = ParticipantInfo.create("session-1", 1L, "socket-1", "익명1");

        // then
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);
        assertThat(participant.getJoinedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("builder - 모든 필드 설정 가능 (create 메소드로 대체)")
    void builder_SetsAllFields() {
        // given
        LocalDateTime now = LocalDateTime.now();

        // when
        ParticipantInfo participant = ParticipantInfo.create(
                "session-1",
                1L,
                "socket-1",
                "익명1"
        );

        // then
        assertThat(participant.getSessionId()).isEqualTo("session-1");
        assertThat(participant.getMemberId()).isEqualTo(1L);
        assertThat(participant.getSocketIds()).containsExactly("socket-1"); // Modified
        assertThat(participant.getAnonymousName()).isEqualTo("익명1");
        // JoinedAt은 create() 시점에 생성되므로, 이제 now와 직접 비교할 수 없음
        // 이 테스트는 create()로 대체되면서 의미가 없어지므로, joinedAt 관련 검증은 제거하거나 다른 방식으로 테스트해야 함.
        // 여기서는 joinedAt 검증을 제거합니다.
        // assertThat(participant.getJoinedAt()).isEqualTo(now);
    }
}