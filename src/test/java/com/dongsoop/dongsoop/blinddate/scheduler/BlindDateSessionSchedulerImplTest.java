package com.dongsoop.dongsoop.blinddate.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.config.BlindDateMessageProvider;
import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateMatchNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.blinddate.support.ManualBlindDateTaskScheduler;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Timeout(15)
class BlindDateSessionSchedulerImplTest {
    private final BlindDateParticipantStorageImpl participants = new BlindDateParticipantStorageImpl();
    private final BlindDateSessionStorageImpl sessions = new BlindDateSessionStorageImpl();
    private final BlindDateEventQueue queue = new BlindDateEventQueue();
    private final ManualBlindDateTaskScheduler time = new ManualBlindDateTaskScheduler();
    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final BlindDateMessageProvider messages = mock(BlindDateMessageProvider.class);
    private final ChatRoomService rooms = mock(ChatRoomService.class);
    private final List<Event> events = new CopyOnWriteArrayList<>();
    private final List<ChatRoom> createdRooms = new CopyOnWriteArrayList<>();
    private final BlindDateChoiceHandler choices = new BlindDateChoiceHandler(
            participants, sessions, messaging, rooms, queue, mock(BlindDateMatchNotification.class));
    private final BlindDateSessionSchedulerImpl scheduler = new BlindDateSessionSchedulerImpl(
            participants, sessions, messages, messaging, time, queue, choices);
    private String id;
    private boolean failParticipants;
    private boolean respondOnParticipants;

    @BeforeEach
    void setUp() {
        id = sessions.create().getSessionId();
        sessions.start(id);
        participants.addParticipant(id, 1L, "one");
        participants.addParticipant(id, 2L, "two");
        when(messages.getStartMessages()).thenReturn(List.of());
        when(messages.getRandomEventMessages(anyInt())).thenReturn(List.of("주제"));
        when(messages.getSessionManagerName()).thenReturn("진행자");
        when(rooms.createOneToOneChatRoom(anyLong(), anyLong(), anyString())).thenAnswer(call -> {
            ChatRoom room = ChatRoom.builder().roomId("room").title(call.getArgument(2)).build();
            createdRooms.add(room);
            return room;
        });
        doAnswer(call -> {
            String destination = call.getArgument(0);
            if (destination.equals(BlindDateTopic.participants(id))) {
                if (failParticipants) {
                    throw new IllegalStateException("delivery failed");
                }
                events.add(new Event(destination, call.getArgument(1)));
                if (respondOnParticipants) {
                    choices.execute(id, 1L, 2L);
                    choices.execute(id, 2L, 1L);
                }
                return null;
            }
            events.add(new Event(destination, call.getArgument(1)));
            return null;
        }).when(messaging).convertAndSend(anyString(), any(Object.class));
    }

    @AfterEach
    void tearDown() {
        queue.shutdown();
    }

    private void reachChoicePeriod() {
        scheduler.start(id);
        time.advanceBy(4_000 + 180_000);
        queue.awaitIdle();
    }

    @Test
    void publicStartPublishesGuidanceConversationAndReadyChoicesInOrder() {
        when(messages.getStartMessages()).thenReturn(List.of("안내"));
        respondOnParticipants = true;
        reachChoicePeriod();
        assertThat(events).extracting(Event::destination).startsWith(
                BlindDateTopic.sessionStart(id), BlindDateTopic.freeze(id), BlindDateTopic.system(id), BlindDateTopic.freeze(id),
                BlindDateTopic.system(id), BlindDateTopic.thaw(id), BlindDateTopic.participants(id));
        assertThat(events).filteredOn(e -> e.destination().equals(BlindDateTopic.system(id)))
                .extracting(e -> ((Map<?, ?>) e.payload()).get("message"))
                .containsExactly("안내", "주제");
        assertThat(events).filteredOn(e -> e.destination().equals(BlindDateTopic.participants(id)))
                .extracting(Event::payload)
                .containsExactly(Map.of("participants", participants.getParticipantsIdAndName(id)));
        assertThat(createdRooms).hasSize(1);
        assertThat(events).filteredOn(e -> e.destination().endsWith("/chatroom")).hasSize(2);
        assertThat(sessions.isProcessing(id)).isTrue();
    }

    @Test
    void choicePeriodLastsThirtySecondsThenOnlySessionEnds() {
        reachChoicePeriod();
        time.advanceBy(29_999);
        queue.awaitIdle();
        assertThat(sessions.isProcessing(id)).isTrue();
        int previousEvents = events.size();
        time.advanceBy(1);
        queue.awaitIdle();
        choices.execute(id, 1L, 2L);
        choices.execute(id, 2L, 1L);
        queue.awaitIdle();
        assertThat(sessions.getState(id)).isNull();
        assertThat(createdRooms).isEmpty();
        assertThat(events).hasSize(previousEvents);
        assertThat(participants.getChoices(id)).isEmpty();
    }

    @Test
    void participantsDeliveryFailureStillAllowsChoicesAndEndsAtDeadline() {
        failParticipants = true;
        reachChoicePeriod();
        choices.execute(id, 1L, 2L);
        choices.execute(id, 2L, 1L);
        queue.awaitIdle();
        assertThat(createdRooms).hasSize(1);
        time.advanceBy(30_000);
        queue.awaitIdle();
        assertThat(sessions.getState(id)).isNull();
        assertThat(events).filteredOn(e -> e.destination().equals(BlindDateTopic.participants(id))).isEmpty();
    }

    @Test
    void endedSessionDoesNotReopenChoicePeriodOrPublishParticipants() {
        scheduler.start(id);
        sessions.terminate(id);
        time.advanceBy(4_000 + 180_000 + 30_000);
        choices.execute(id, 1L, 2L);
        choices.execute(id, 2L, 1L);
        queue.awaitIdle();
        assertThat(sessions.getState(id)).isNull();
        assertThat(createdRooms).isEmpty();
        assertThat(participants.getChoices(id)).isEmpty();
        assertThat(events).filteredOn(e -> e.destination().equals(BlindDateTopic.participants(id))).isEmpty();
    }

    @Test
    void failedDeadlineRegistrationClosesSessionWithoutMatching() {
        time.rejectDelay(30_000);
        reachChoicePeriod();
        choices.execute(id, 1L, 2L);
        choices.execute(id, 2L, 1L);
        queue.awaitIdle();
        assertThat(sessions.getState(id)).isNull();
        assertThat(createdRooms).isEmpty();
        assertThat(participants.getChoices(id)).isEmpty();
        assertThat(events).filteredOn(e -> e.destination().endsWith("/chatroom") || e.destination().endsWith("/failed")).isEmpty();
    }

    private record Event(String destination, Object payload) {}
}
