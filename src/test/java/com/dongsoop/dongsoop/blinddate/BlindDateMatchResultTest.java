package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Timeout(15)
@DisplayName("과팅 전원 응답 후 일괄 결과 확정")
class BlindDateMatchResultTest {
    private final BlindDateEventQueue queue = new BlindDateEventQueue();
    private final BlindDateParticipantStorageImpl participants = new BlindDateParticipantStorageImpl();
    private final BlindDateSessionStorageImpl sessions = new BlindDateSessionStorageImpl();
    private final ChatRoomService rooms = mock(ChatRoomService.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final List<Event> events = new CopyOnWriteArrayList<>();
    private final List<List<Long>> pairs = new CopyOnWriteArrayList<>();
    private BlindDateChoiceHandler handler;
    private String sessionId;

    @BeforeEach
    void setUp() {
        handler = new BlindDateChoiceHandler(participants, sessions, messaging, rooms, queue);
        sessionId = openSession(1, 3);
        doAnswer(call -> {
            events.add(new Event(call.getArgument(0), call.getArgument(1)));
            return null;
        }).when(messaging).convertAndSend(anyString(), any(Object.class));
        when(rooms.createOneToOneChatRoom(anyLong(), anyLong(), anyString())).thenAnswer(call -> {
            pairs.add(List.of(call.getArgument(0), call.getArgument(1)));
            return ChatRoom.builder().roomId("room-" + pairs.size()).build();
        });
    }

    @AfterEach
    void tearDown() {
        queue.shutdown();
    }

    private String openSession(long first, long last) {
        String id = sessions.create().getSessionId();
        sessions.start(id);
        for (long member = first; member <= last; member++) {
            participants.addParticipant(id, member, "socket-" + member);
        }
        participants.openChoices(id);
        queue.openChoices(id);
        return id;
    }

    @Test
    void mutualChoicesWaitForLastNoChoiceResponse() {
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        assertThat(events).isEmpty();
        assertThat(pairs).isEmpty();
        assertThat(sessions.isProcessing(sessionId)).isTrue();
        assertThat(participants.isMatched(sessionId, 1L)).isFalse();

        handler.execute(sessionId, 3L, null);
        queue.awaitIdle();
        assertThat(pairs).containsExactly(List.of(1L, 2L));
        assertThat(events).containsExactly(
                success(1L, "room-1"), success(2L, "room-1"), failure(3L));
        assertThat(sessions.getState(sessionId)).isNull();
    }

    @Test
    void noChoicesAndLateChangedResponsesProduceOnlyFailures() {
        handler.execute(sessionId, 1L, null);
        handler.execute(sessionId, 1L, 2L); // 미선택도 최초 응답으로 확정
        handler.execute(sessionId, 2L, null);
        handler.execute(sessionId, 3L, null);
        queue.awaitIdle();
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        assertThat(pairs).isEmpty();
        assertThat(events).containsExactlyInAnyOrder(failure(1L), failure(2L), failure(3L));
    }

    @Test
    void circularChoicesProduceAllFailures() {
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 3L);
        handler.execute(sessionId, 3L, 1L);
        queue.awaitIdle();
        assertThat(events).containsExactlyInAnyOrder(failure(1L), failure(2L), failure(3L));
        assertThat(pairs).isEmpty();
    }

    @Test
    void duplicateResponsesDoNotCountAsMissingMemberOrReplaceFirstChoice() {
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 1L, 3L);
        handler.execute(sessionId, 1L, null);
        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        assertThat(events).isEmpty();
        handler.execute(sessionId, 3L, null);
        queue.awaitIdle();
        assertThat(pairs).containsExactly(List.of(1L, 2L));
        assertThat(events).hasSize(3);
    }

    @Test
    void delayedRoomCreationAndDuplicateFinalResponsesDoNotRunTwice() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(rooms.createOneToOneChatRoom(anyLong(), anyLong(), anyString())).thenAnswer(call -> {
            started.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            pairs.add(List.of(call.getArgument(0), call.getArgument(1)));
            return ChatRoom.builder().roomId("room-1").build();
        });
        try {
            handler.execute(sessionId, 1L, 2L);
            handler.execute(sessionId, 2L, 1L);
            handler.execute(sessionId, 3L, null);
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            for (int i = 0; i < 30; i++) {
                handler.execute(sessionId, 3L, null);
                handler.execute(sessionId, 2L, 1L);
            }
            assertThat(events).isEmpty();
            assertThat(sessions.isProcessing(sessionId)).isTrue();
        } finally {
            release.countDown();
        }
        queue.awaitIdle();
        assertThat(pairs).hasSize(1);
        assertThat(events).hasSize(3);
    }

    @Test
    void oneSessionsMissingResponseDoesNotBlockAnotherSession() {
        String other = openSession(4, 5);
        handler.execute(sessionId, 1L, null);
        handler.execute(other, 4L, null);
        handler.execute(other, 5L, null);
        queue.awaitIdle();
        assertThat(sessions.isProcessing(sessionId)).isTrue();
        assertThat(sessions.getState(other)).isNull();
        assertThat(events).extracting(Event::destination).containsExactlyInAnyOrder(
                BlindDateTopic.matchFailed(other, 4L), BlindDateTopic.matchFailed(other, 5L));
    }

    @Test
    void onePublishFailureDoesNotPreventOtherResultsOrTermination() {
        doAnswer(call -> {
            if (call.getArgument(0).equals(BlindDateTopic.matchFailed(sessionId, 1L))) {
                throw new IllegalStateException("delivery failed");
            }
            events.add(new Event(call.getArgument(0), call.getArgument(1)));
            return null;
        }).when(messaging).convertAndSend(anyString(), any(Object.class));
        for (long member = 1; member <= 3; member++) {
            handler.execute(sessionId, member, null);
        }
        queue.awaitIdle();
        assertThat(events).containsExactlyInAnyOrder(failure(2L), failure(3L));
        assertThat(sessions.getState(sessionId)).isNull();
    }

    @Test
    void roomCreationFailureProducesFailuresWithoutRepeatingCreation() {
        when(rooms.createOneToOneChatRoom(anyLong(), anyLong(), anyString()))
                .thenAnswer(call -> {
                    pairs.add(List.of(call.getArgument(0), call.getArgument(1)));
                    throw new IllegalStateException("room failed");
                });
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 1L);
        handler.execute(sessionId, 3L, null);
        queue.awaitIdle();
        handler.execute(sessionId, 3L, null);
        queue.awaitIdle();
        assertThat(pairs).hasSize(1);
        assertThat(events).containsExactlyInAnyOrder(failure(1L), failure(2L), failure(3L));
        assertThat(sessions.getState(sessionId)).isNull();
    }

    private Event success(long member, String room) {
        return new Event(BlindDateTopic.chatRoomCreated(sessionId, member), Map.of("chatRoomId", room));
    }

    private Event failure(long member) {
        return new Event(BlindDateTopic.matchFailed(sessionId, member), Map.of("message", "매칭에 실패했습니다."));
    }

    private record Event(String destination, Object payload) {}
}
