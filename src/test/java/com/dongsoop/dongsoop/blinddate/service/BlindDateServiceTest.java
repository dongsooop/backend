package com.dongsoop.dongsoop.blinddate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;
import com.dongsoop.dongsoop.blinddate.support.ManualBlindDateTaskScheduler;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class BlindDateServiceTest {
    private final BlindDateParticipantStorageImpl participants = new BlindDateParticipantStorageImpl();
    private final BlindDateSessionStorageImpl sessions = new BlindDateSessionStorageImpl();
    private final BlindDateStorageImpl operation = new BlindDateStorageImpl();
    private final BlindDateEventQueue queue = new BlindDateEventQueue();
    private final ManualBlindDateTaskScheduler time = new ManualBlindDateTaskScheduler();
    private final BlindDateNotification notification = mock(BlindDateNotification.class);
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final List<Event> events = new ArrayList<>();
    private final BlindDateServiceImpl service = new BlindDateServiceImpl(
            participants, operation, notification, sessions, messaging, time, queue);

    @BeforeEach
    void setUp() {
        doAnswer(call -> {
            events.add(new Event(call.getArgument(0), call.getArgument(1)));
            return null;
        }).when(messaging).convertAndSend(anyString(), any(Object.class));
    }

    @AfterEach
    void tearDown() {
        queue.shutdown();
    }

    @Test
    void startChangesAvailabilityAndCapacity() {
        assertThat(service.isAvailable()).isFalse();
        service.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusHours(1), 5));
        assertThat(service.isAvailable()).isTrue();
        assertThat(operation.getMaxSessionMemberCount()).isEqualTo(5);
    }

    @Test
    void notificationFailureDoesNotPreventStart() {
        doThrow(new IllegalStateException("delivery failed")).when(notification).send();
        service.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusHours(1), 5));
        assertThat(service.isAvailable()).isTrue();
        assertThat(operation.getMaxSessionMemberCount()).isEqualTo(5);
    }

    @Test
    void timerFailureLeavesOperationClosed() {
        time.rejectAbsoluteSchedules();
        assertThatThrownBy(() -> service.startBlindDate(
                new StartBlindDateRequest(LocalDateTime.now().plusHours(1), 5)))
                .isInstanceOf(RuntimeException.class);
        assertThat(service.isAvailable()).isFalse();
        assertThat(operation.getPointer()).isNull();
    }

    @Test
    void resetDropsAllParticipationAndChoicesButKeepsOperationAndExpiry() {
        service.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusMinutes(1), 5));
        String id = sessions.create().getSessionId();
        operation.setPointer(id);
        participants.addParticipant(id, 1L, "one");
        participants.openChoices(id);
        participants.recordChoice(id, 1L, null);
        service.resetParticipants();
        queue.awaitIdle();
        assertThat(service.isAvailable()).isTrue();
        assertThat(operation.getMaxSessionMemberCount()).isEqualTo(5);
        assertThat(operation.getPointer()).isNull();
        assertThat(sessions.getState(id)).isNull();
        assertThat(participants.getByMemberId(1L)).isNull();
        assertThat(participants.getBySocketId("one")).isNull();
        assertThat(participants.getChoices(id)).isEmpty();

        time.advanceBy(120_000);
        assertThat(service.isAvailable()).isFalse();
    }

    @Test
    void expirationClosesNewEntryBeforeDelayedRecordCleanup() {
        service.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusMinutes(1), 5));
        String id = sessions.create().getSessionId();
        participants.addParticipant(id, 1L, "one");
        time.advanceBy(120_000);
        assertThat(service.isAvailable()).isFalse();
        assertThat(participants.getByMemberId(1L)).isNotNull();
        time.advanceBy(30 * 60_000);
        queue.awaitIdle();
        assertThat(sessions.getState(id)).isNull();
        assertThat(participants.getByMemberId(1L)).isNull();
    }

    @Test
    void joinedCountPublishesSessionAndPayload() {
        service.broadcastJoinedCount("session", 5);
        assertThat(events).containsExactly(new Event(BlindDateTopic.joined("session"), Map.of("volunteer", 5)));
    }

    private record Event(String destination, Object payload) {}
}
