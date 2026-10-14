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
import com.dongsoop.dongsoop.blinddate.handler.BlindDateDisconnectHandler;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateMatchNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.blinddate.service.BlindDateService;
import com.dongsoop.dongsoop.blinddate.service.BlindDateServiceImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.support.ManualBlindDateTaskScheduler;
import java.time.LocalDateTime;
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
@DisplayName("과팅 최종 선택 순차 처리와 즉시 성공 알림")
class BlindDateMatchResultTest {
    private final BlindDateEventQueue queue = new BlindDateEventQueue();
    private final BlindDateParticipantStorageImpl participants = new BlindDateParticipantStorageImpl();
    private final BlindDateSessionStorageImpl sessions = new BlindDateSessionStorageImpl(event -> {});
    private final ChatRoomService rooms = mock(ChatRoomService.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final BlindDateMatchNotification notification = mock(BlindDateMatchNotification.class);
    private final List<Event> events = new CopyOnWriteArrayList<>();
    private final List<Event> notifications = new CopyOnWriteArrayList<>();
    private final List<List<Long>> pairs = new CopyOnWriteArrayList<>();
    private BlindDateChoiceHandler handler;
    private String sessionId;

    @BeforeEach
    void setUp() {
        handler = new BlindDateChoiceHandler(participants, sessions, messaging, rooms, queue, notification);
        sessionId = openSession(1, 3);
        doAnswer(call -> {
            events.add(new Event(call.getArgument(0), call.getArgument(1)));
            return null;
        }).when(messaging).convertAndSend(anyString(), any(Object.class));
        doAnswer(call -> {
            notifications.add(new Event("member-" + call.getArgument(0), call.getArgument(1)));
            return null;
        }).when(notification).send(anyLong(), anyString());
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
    void mutualChoicesNotifyImmediatelyWithoutThirdMembersResponse() {
        handler.execute(sessionId, 1L, 2L);
        queue.awaitIdle();
        assertThat(events).isEmpty();
        assertThat(pairs).isEmpty();

        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        assertThat(pairs).containsExactly(List.of(1L, 2L));
        assertThat(events).containsExactlyInAnyOrder(success(sessionId, 1L, "room-1"), success(sessionId, 2L, "room-1"));
        assertThat(notifications).containsExactlyInAnyOrder(new Event("member-1", "room-1"), new Event("member-2", "room-1"));
        assertThat(participants.getChoices(sessionId)).doesNotContainKey(3L);
        assertThat(sessions.isProcessing(sessionId)).isTrue();

        handler.execute(sessionId, 3L, null);
        queue.awaitIdle();
        assertThat(pairs).hasSize(1);
        assertThat(events).hasSize(2);
        assertThat(sessions.isProcessing(sessionId)).isTrue();
        handler.timeout(sessionId);
        queue.awaitIdle();
        assertThat(sessions.getState(sessionId)).isNull();
    }

    @Test
    void noChoicesAndLateChangedResponsesProduceNoResults() {
        handler.execute(sessionId, 1L, null);
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, null);
        handler.execute(sessionId, 3L, null);
        queue.awaitIdle();
        handler.execute(sessionId, 2L, 1L);
        handler.timeout(sessionId);
        queue.awaitIdle();
        assertThat(pairs).isEmpty();
        assertThat(events).isEmpty();
        assertThat(notifications).isEmpty();
        assertThat(sessions.getState(sessionId)).isNull();
    }

    @Test
    void circularChoicesNeverMatchOrEmitFailures() {
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 3L);
        handler.execute(sessionId, 3L, 1L);
        handler.timeout(sessionId);
        queue.awaitIdle();
        assertThat(events).isEmpty();
        assertThat(notifications).isEmpty();
        assertThat(pairs).isEmpty();
        assertThat(sessions.getState(sessionId)).isNull();
    }

    @Test
    void duplicateResponsesDoNotReplaceFirstChoiceOrRepeatSuccessfulPair() {
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 1L, 3L);
        handler.execute(sessionId, 1L, null);
        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        for (int i = 0; i < 30; i++) {
            handler.execute(sessionId, 1L, 2L);
            handler.execute(sessionId, 2L, 1L);
        }
        queue.awaitIdle();
        assertThat(pairs).containsExactly(List.of(1L, 2L));
        assertThat(events).hasSize(2);
        assertThat(notifications).hasSize(2);
    }

    @Test
    void timeoutDuringSlowMatchWaitsForAcceptedWorkAndRejectsNewRequests() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        doAnswer(call -> {
            started.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            pairs.add(List.of(call.getArgument(0), call.getArgument(1)));
            return ChatRoom.builder().roomId("room-1").build();
        }).when(rooms).createOneToOneChatRoom(anyLong(), anyLong(), anyString());
        try {
            handler.execute(sessionId, 1L, 2L);
            handler.execute(sessionId, 2L, 1L);
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            handler.timeout(sessionId);
            handler.execute(sessionId, 3L, 1L);
            handler.timeout(sessionId);
            assertThat(events).isEmpty();
            assertThat(sessions.isProcessing(sessionId)).isTrue();
        } finally {
            release.countDown();
        }
        queue.awaitIdle();
        assertThat(pairs).hasSize(1);
        assertThat(events).hasSize(2);
        assertThat(notifications).hasSize(2);
        assertThat(participants.getChoices(sessionId)).doesNotContainKey(3L);
        assertThat(sessions.getState(sessionId)).isNull();
    }

    @Test
    void oneSessionsMissingResponseDoesNotBlockAnotherSessionsImmediateMatch() {
        String other = openSession(4, 5);
        handler.execute(sessionId, 1L, null);
        handler.execute(other, 4L, 5L);
        handler.execute(other, 5L, 4L);
        queue.awaitIdle();
        assertThat(sessions.isProcessing(sessionId)).isTrue();
        assertThat(events).containsExactlyInAnyOrder(success(other, 4L, "room-1"), success(other, 5L, "room-1"));
        assertThat(pairs).containsExactly(List.of(4L, 5L));
    }

    @Test
    void onePublishFailureDoesNotPreventNotificationsOrOtherResults() {
        doAnswer(call -> {
            if (call.getArgument(0).equals(BlindDateTopic.chatRoomCreated(sessionId, 1L))) {
                throw new IllegalStateException("delivery failed");
            }
            events.add(new Event(call.getArgument(0), call.getArgument(1)));
            return null;
        }).when(messaging).convertAndSend(anyString(), any(Object.class));
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 1L);
        handler.timeout(sessionId);
        queue.awaitIdle();
        assertThat(events).containsExactly(success(sessionId, 2L, "room-1"));
        assertThat(notifications).hasSize(2);
        assertThat(sessions.getState(sessionId)).isNull();
    }

    @Test
    void roomCreationFailureIsNotRetriedByDuplicatesOrTimeout() {
        doAnswer(call -> {
            pairs.add(List.of(call.getArgument(0), call.getArgument(1)));
            throw new IllegalStateException("room failed");
        }).when(rooms).createOneToOneChatRoom(anyLong(), anyLong(), anyString());
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 1L);
        handler.timeout(sessionId);
        queue.awaitIdle();
        assertThat(pairs).hasSize(1);
        assertThat(events).isEmpty();
        assertThat(notifications).isEmpty();
        assertThat(sessions.getState(sessionId)).isNull();
    }

    @Test
    void timeoutOnlyTerminatesWithoutFillingMissingChoicesOrEmittingResults() {
        handler.execute(sessionId, 1L, 2L);
        queue.awaitIdle();
        handler.timeout(sessionId);
        handler.timeout(sessionId);
        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        assertThat(participants.getChoices(sessionId)).containsOnlyKeys(1L).containsEntry(1L, 2L);
        assertThat(pairs).isEmpty();
        assertThat(events).isEmpty();
        assertThat(notifications).isEmpty();
        assertThat(sessions.getState(sessionId)).isNull();
    }

    @Test
    void endedSessionRejectsMatchingEvenIfChoiceQueueWasLeftOpen() {
        handler.execute(sessionId, 1L, 2L);
        queue.awaitIdle();
        sessions.terminate(sessionId);
        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        assertThat(participants.getChoices(sessionId)).doesNotContainKey(2L);
        assertThat(pairs).isEmpty();
        assertThat(events).isEmpty();
        assertThat(notifications).isEmpty();
    }

    @Test
    void completedMatchThenRepeatedTimeoutNeverRepeatsResults() {
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        handler.timeout(sessionId);
        handler.timeout(sessionId);
        queue.awaitIdle();
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        assertThat(pairs).hasSize(1);
        assertThat(events).hasSize(2);
        assertThat(notifications).hasSize(2);
    }

    @Test
    void disconnectionDoesNotDiscardSubmittedChoiceOrRequireAllResponses() {
        var disconnect = new BlindDateDisconnectHandler(participants, sessions, mock(BlindDateService.class), queue);
        handler.execute(sessionId, 1L, 2L);
        queue.awaitIdle();
        disconnect.execute("socket-1", 1L, sessionId);
        disconnect.execute("socket-3", 3L, sessionId);
        queue.awaitIdle();
        handler.execute(sessionId, 2L, 1L);
        queue.awaitIdle();
        assertThat(notifications).containsExactlyInAnyOrder(new Event("member-1", "room-1"), new Event("member-2", "room-1"));
        assertThat(events).hasSize(2);
        assertThat(participants.getChoices(sessionId)).doesNotContainKey(3L);
    }

    @Test
    void oneNotificationFailureDoesNotPreventOtherNotificationOrTermination() {
        doAnswer(call -> {
            if (call.getArgument(0).equals(1L)) {
                throw new IllegalStateException("notification failed");
            }
            notifications.add(new Event("member-" + call.getArgument(0), call.getArgument(1)));
            return null;
        }).when(notification).send(anyLong(), anyString());
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 1L);
        handler.timeout(sessionId);
        queue.awaitIdle();
        assertThat(events).hasSize(2);
        assertThat(notifications).containsExactly(new Event("member-2", "room-1"));
        assertThat(sessions.getState(sessionId)).isNull();
    }

    @Test
    void failedPairDoesNotPreventAnotherPairsImmediateSuccess() {
        String other = openSession(4, 8);
        doAnswer(call -> {
            Long memberId = call.getArgument(0);
            pairs.add(List.of(memberId, call.getArgument(1)));
            if (memberId.equals(4L)) {
                throw new IllegalStateException("room failed");
            }
            return ChatRoom.builder().roomId("successful-room").build();
        }).when(rooms).createOneToOneChatRoom(anyLong(), anyLong(), anyString());
        handler.execute(other, 4L, 5L);
        handler.execute(other, 5L, 4L);
        handler.execute(other, 6L, 7L);
        handler.execute(other, 7L, 6L);
        queue.awaitIdle();
        assertThat(pairs).containsExactly(List.of(4L, 5L), List.of(6L, 7L));
        assertThat(events).containsExactlyInAnyOrder(success(other, 6L, "successful-room"), success(other, 7L, "successful-room"));
        assertThat(notifications).containsExactlyInAnyOrder(new Event("member-6", "successful-room"), new Event("member-7", "successful-room"));
        assertThat(participants.getChoices(other)).doesNotContainKey(8L);
        handler.timeout(other);
        queue.awaitIdle();
        assertThat(sessions.getState(other)).isNull();
    }

    @Test
    void resetDuringMatchFinishesAcceptedResultThenClearsRecordsAndRejectsLateChoices() throws Exception {
        var operation = new BlindDateStorageImpl();
        operation.start(3, LocalDateTime.now().plusHours(1));
        operation.setPointer(sessionId);
        var service = new BlindDateServiceImpl(participants, operation, mock(BlindDateNotification.class),
                sessions, messaging, new ManualBlindDateTaskScheduler(), queue);
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        doAnswer(call -> {
            started.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            pairs.add(List.of(call.getArgument(0), call.getArgument(1)));
            return ChatRoom.builder().roomId("room-1").build();
        }).when(rooms).createOneToOneChatRoom(anyLong(), anyLong(), anyString());
        try {
            handler.execute(sessionId, 1L, 2L);
            handler.execute(sessionId, 2L, 1L);
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            service.resetParticipants();
            handler.execute(sessionId, 3L, 1L);
            assertThat(sessions.isProcessing(sessionId)).isTrue();
            assertThat(events).isEmpty();
        } finally {
            release.countDown();
        }
        queue.awaitIdle();
        assertThat(pairs).containsExactly(List.of(1L, 2L));
        assertThat(events).containsExactlyInAnyOrder(success(sessionId, 1L, "room-1"), success(sessionId, 2L, "room-1"));
        assertThat(notifications).containsExactlyInAnyOrder(new Event("member-1", "room-1"), new Event("member-2", "room-1"));
        assertThat(sessions.getState(sessionId)).isNull();
        assertThat(operation.getPointer()).isNull();
        assertThat(operation.isAvailable()).isTrue();
        assertThat(operation.getMaxSessionMemberCount()).isEqualTo(3);
        for (long member = 1; member <= 3; member++) {
            assertThat(participants.getByMemberId(member)).isNull();
            assertThat(participants.getBySocketId("socket-" + member)).isNull();
        }
        assertThat(participants.getChoices(sessionId)).isEmpty();
        handler.execute(sessionId, 1L, 2L);
        handler.execute(sessionId, 2L, 1L);
        handler.timeout(sessionId);
        queue.awaitIdle();
        assertThat(pairs).hasSize(1);
        assertThat(events).hasSize(2);
        assertThat(notifications).hasSize(2);
        String fresh = openSession(1, 3);
        handler.execute(fresh, 1L, 2L);
        handler.execute(fresh, 2L, 1L);
        queue.awaitIdle();
        assertThat(pairs).containsExactly(List.of(1L, 2L), List.of(1L, 2L));
        assertThat(events).contains(success(fresh, 1L, "room-1"), success(fresh, 2L, "room-1"));
        assertThat(notifications).hasSize(4);
    }

    private Event success(String id, long member, String room) {
        return new Event(BlindDateTopic.chatRoomCreated(id, member), Map.of("chatRoomId", room));
    }

    private record Event(String destination, Object payload) {}
}
