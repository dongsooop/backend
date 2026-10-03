package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Timeout(15)
class BlindDateChoiceErrorScenarioTest {
    @ParameterizedTest
    @CsvSource({
        "1, 1, 400, INVALID_CHOICE",
        "1, , 400, INVALID_CHOICE",
        "99, 1, 403, CHOICE_FORBIDDEN",
        "3, 1, 403, CHOICE_FORBIDDEN",
        "1, 99, 404, CHOICE_TARGET_NOT_FOUND",
        "1, 3, 404, CHOICE_TARGET_NOT_FOUND"
    })
    void sendErrorThenAllowNormalMatch(Long chooser, Long target, int status, String code) {
        var queue = new BlindDateEventQueue();
        try {
            var participants = new BlindDateParticipantStorageImpl();
            participants.addParticipant("session", 1L, "one");
            participants.addParticipant("session", 2L, "two");
            participants.addParticipant("other", 3L, "three");
            var sessions = mock(BlindDateSessionStorage.class);
            when(sessions.isProcessing("session")).thenReturn(true);
            var messaging = mock(SimpMessagingTemplate.class);
            List<Event> events = new CopyOnWriteArrayList<>();
            doAnswer(
                            call -> {
                                events.add(new Event(call.getArgument(0), call.getArgument(1)));
                                return null;
                            })
                    .when(messaging)
                    .convertAndSend(anyString(), any(Object.class));
            var rooms = mock(ChatRoomService.class);
            List<List<Long>> pairs = new CopyOnWriteArrayList<>();
            when(rooms.createOneToOneChatRoom(anyLong(), anyLong(), anyString()))
                    .thenAnswer(
                            call -> {
                                pairs.add(List.of(call.getArgument(0), call.getArgument(1)));
                                return ChatRoom.builder().roomId("room").build();
                            });
            var handler =
                    new BlindDateChoiceHandler(participants, sessions, messaging, rooms, queue);
            queue.openChoices("session", 60_000);

            handler.execute("session", chooser, target);
            queue.awaitIdle();
            assertThat(pairs).isEmpty();
            assertThat(events).hasSize(1);
            assertThat(events.get(0).destination())
                    .isEqualTo(
                            "/topic/blinddate/session/session/member/" + chooser + "/choice-error");
            assertThat(events.get(0).payload())
                    .containsEntry("status", status)
                    .containsEntry("code", code);
            assertThat(events.get(0).payload().get("message")).isInstanceOf(String.class);
            assertThat(participants.getNotMatched("session")).containsExactlyInAnyOrder(1L, 2L);

            handler.execute("session", 1L, 2L);
            handler.execute("session", 2L, 1L);
            queue.awaitIdle();
            assertThat(pairs).hasSize(1);
            assertThat(pairs.get(0)).containsExactlyInAnyOrder(1L, 2L);
            assertThat(events).hasSize(3);
            assertThat(events.subList(1, 3))
                    .extracting(Event::destination)
                    .containsExactlyInAnyOrder(
                            "/topic/blinddate/session/session/member/1/chatroom",
                            "/topic/blinddate/session/session/member/2/chatroom");
            assertThat(participants.getNotMatched("session")).isEmpty();
            assertThat(participants.getNotMatched("other")).containsExactly(3L);
        } finally {
            queue.shutdown();
        }
    }

    private record Event(String destination, Map<String, Object> payload) {}
}
