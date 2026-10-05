package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.support.BlindDateScenarioFixture;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(15)
@DisplayName("과팅 외부 경계 실패 이후 진행과 종료")
class BlindDateFailureScenarioTest {
    @Test
    @DisplayName("F05 한 쌍의 채팅방 생성 실패는 다른 쌍과 종료를 막지 않음")
    void roomCreationFailureDoesNotBlockOtherPairs() {
        try (var flow = new BlindDateScenarioFixture(5)) {
            when(flow.rooms.createOneToOneChatRoom(anyLong(), anyLong(), anyString()))
                    .thenThrow(new IllegalStateException("room unavailable"))
                    .thenReturn(ChatRoom.builder().roomId("other-room").build());
            String session = flow.joinMembers(5);
            flow.openChoices(session);
            flow.choice.execute(session, 1L, 2L);
            flow.choice.execute(session, 2L, 1L);
            flow.choice.execute(session, 3L, 4L);
            flow.choice.execute(session, 4L, 3L);
            flow.finishChoices();
            assertThat(flow.recipients("/chatroom")).containsExactlyInAnyOrder(3L, 4L);
            assertThat(flow.recipients("/failed")).contains(5L);
            assertThat(flow.sessions.getState(session)).isNull();
        }
    }

    @Test
    @DisplayName("F06 한 성공 전송 실패는 이후 다른 매칭과 종료를 막지 않음")
    void successDeliveryFailureDoesNotBlockOtherPairs() {
        try (var flow = new BlindDateScenarioFixture(5)) {
            String session = flow.joinMembers(5);
            flow.openChoices(session);
            doThrow(new IllegalStateException("delivery failed"))
                    .when(flow.messaging)
                    .convertAndSend(endsWith("/member/2/chatroom"), any(Object.class));
            flow.choice.execute(session, 1L, 2L);
            flow.choice.execute(session, 2L, 1L);
            flow.choice.execute(session, 3L, 4L);
            flow.choice.execute(session, 4L, 3L);
            flow.finishChoices();
            assertThat(flow.recipients("/chatroom")).contains(3L, 4L);
            assertThat(flow.recipients("/failed")).contains(5L);
            assertThat(flow.sessions.getState(session)).isNull();
        }
    }

    @Test
    @DisplayName("F07 모든 실패 전송이 예외여도 세션 종료와 늦은 선택 차단")
    void failedDeliveryStillTerminatesAndRejectsLateChoice() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            flow.openChoices(session);
            doThrow(new IllegalStateException("delivery failed"))
                    .when(flow.messaging)
                    .convertAndSend(endsWith("/failed"), any(Object.class));
            flow.finishChoices();
            flow.choice.execute(session, 1L, 2L);
            flow.choice.execute(session, 2L, 1L);
            flow.queue.awaitIdle();
            assertThat(flow.sessions.getState(session)).isNull();
            assertThat(flow.createdPairs).isEmpty();
            assertThat(flow.recipients("/chatroom")).isEmpty();
        }
    }

    @Test
    @DisplayName("F08 참가자 목록 전송 예외에도 선택 마감과 종료")
    void participantListFailureStillSchedulesFinalization() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            doThrow(new IllegalStateException("delivery failed"))
                    .when(flow.messaging)
                    .convertAndSend(endsWith("/participants"), any(Object.class));
            flow.openChoices(session);
            flow.finishChoices();
            assertThat(flow.recipients("/failed")).containsExactlyInAnyOrder(1L, 2L);
            assertThat(flow.sessions.getState(session)).isNull();
        }
    }
}
