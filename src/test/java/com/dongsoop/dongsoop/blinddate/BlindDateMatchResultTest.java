package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.config.BlindDateMessageProvider;
import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateSessionSchedulerImpl;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("과팅 매칭 결과 이벤트 배타성")
class BlindDateMatchResultTest {

    private final BlindDateEventQueue eventQueue = new BlindDateEventQueue();
    private final Queue<Runnable> timers = new ArrayDeque<>();
    private final List<ResultEvent> events = new CopyOnWriteArrayList<>();
    private final ChatRoomService chatRoomService = mock(ChatRoomService.class);
    private BlindDateChoiceHandler choiceHandler;
    private String sessionId;
    private boolean failAfterFirstFailureEvent;

    @BeforeEach
    void setUp() {
        var participants = new BlindDateParticipantStorageImpl();
        var sessions = new BlindDateSessionStorageImpl();
        sessionId = sessions.create().getSessionId();
        sessions.start(sessionId);
        for (long memberId = 1; memberId <= 3; memberId++) {
            participants.addParticipant(sessionId, memberId, "socket-" + memberId);
        }

        var messaging = mock(SimpMessagingTemplate.class);
        doAnswer(invocation -> {
            String destination = invocation.getArgument(0);
            if (failAfterFirstFailureEvent && destination.endsWith("/failed") && !events.isEmpty()) {
                throw new IllegalStateException("Failure event delivery failed");
            }
            if (destination.endsWith("/chatroom") || destination.endsWith("/failed")) {
                events.add(new ResultEvent(destination, invocation.getArgument(1)));
            }
            return null;
        }).when(messaging).convertAndSend(anyString(), any(Object.class));

        var taskScheduler = mock(BlindDateTaskScheduler.class);
        doAnswer(invocation -> {
            timers.add(invocation.getArgument(0));
            return null;
        }).when(taskScheduler).schedule(any(Runnable.class), anyLong());

        var messages = mock(BlindDateMessageProvider.class);
        when(messages.getStartMessages()).thenReturn(List.of());
        when(messages.getRandomEventMessages(1)).thenReturn(List.of("event"));
        when(messages.getSessionManagerName()).thenReturn("manager");
        var scheduler = new BlindDateSessionSchedulerImpl(
                participants, sessions, messages, messaging, taskScheduler, eventQueue);
        ReflectionTestUtils.setField(scheduler, "eventMessageAmount", 1);
        choiceHandler = new BlindDateChoiceHandler(
                participants, sessions, messaging, chatRoomService, eventQueue);

        // 공개 시작 흐름을 실행하고 가상 타이머를 진행해 선택 단계에 도달한다.
        scheduler.start(sessionId);
        timers.remove().run();
        timers.remove().run();
        assertThat(events).isEmpty();
    }

    @AfterEach
    void tearDown() {
        eventQueue.shutdown();
    }

    @Test
    @DisplayName("상호 선택한 참가자는 성공만, 미매칭 참가자는 실패만 받는다")
    void matchedMembersReceiveOnlySuccess() {
        when(chatRoomService.createOneToOneChatRoom(anyLong(), anyLong(), anyString()))
                .thenReturn(ChatRoom.builder().roomId("room-1").build());

        choiceHandler.execute(sessionId, 1L, 2L);
        choiceHandler.execute(sessionId, 2L, 1L);
        choiceHandler.execute(sessionId, 2L, 1L); // 중복 선택도 결과를 추가하지 않는다.
        timers.remove().run();
        eventQueue.awaitIdle();

        assertSuccessfulPairAndUnmatchedMember();
        assertThat(events.get(2)).isEqualTo(failedEvent(3L));
    }

    @Test
    @DisplayName("채팅방 생성이 지연돼도 실패 판정은 매칭 성공 처리를 추월하지 않는다")
    void slowChatRoomCreationDoesNotProduceFailureBeforeSuccess() throws Exception {
        var creationStarted = new CountDownLatch(1);
        var allowCreation = new CountDownLatch(1);
        when(chatRoomService.createOneToOneChatRoom(anyLong(), anyLong(), anyString()))
                .thenAnswer(invocation -> {
                    creationStarted.countDown();
                    if (!allowCreation.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Chat room creation was not released");
                    }
                    return ChatRoom.builder().roomId("room-1").build();
                });

        try {
            choiceHandler.execute(sessionId, 1L, 2L);
            choiceHandler.execute(sessionId, 2L, 1L);
            assertThat(creationStarted.await(5, TimeUnit.SECONDS)).isTrue();
            timers.remove().run();
        } finally {
            allowCreation.countDown();
        }
        eventQueue.awaitIdle();

        assertSuccessfulPairAndUnmatchedMember();
        assertThat(events.get(2)).isEqualTo(failedEvent(3L));
    }

    @Test
    @DisplayName("종료 후 늦은 상호 선택은 실패 결과를 성공으로 뒤집지 않는다")
    void choicesAfterFinalizationDoNotProduceSuccess() {
        when(chatRoomService.createOneToOneChatRoom(anyLong(), anyLong(), anyString()))
                .thenReturn(ChatRoom.builder().roomId("room-1").build());

        timers.remove().run();
        choiceHandler.execute(sessionId, 1L, 2L);
        choiceHandler.execute(sessionId, 2L, 1L);
        eventQueue.awaitIdle();

        assertThat(events).containsExactlyInAnyOrder(
                failedEvent(1L), failedEvent(2L), failedEvent(3L));
    }

    private void assertSuccessfulPairAndUnmatchedMember() {
        assertThat(events).containsExactlyInAnyOrder(
                new ResultEvent(BlindDateTopic.chatRoomCreated(sessionId, 1L), Map.of("chatRoomId", "room-1")),
                new ResultEvent(BlindDateTopic.chatRoomCreated(sessionId, 2L), Map.of("chatRoomId", "room-1")),
                failedEvent(3L));
    }

    @Test
    @DisplayName("실패 알림 전송 중 예외가 발생해도 늦은 선택에서 성공 이벤트를 보내지 않는다")
    void failureDeliveryExceptionDoesNotAllowLateMatch() {
        when(chatRoomService.createOneToOneChatRoom(anyLong(), anyLong(), anyString()))
                .thenReturn(ChatRoom.builder().roomId("room-1").build());
        failAfterFirstFailureEvent = true;
        timers.remove().run();
        eventQueue.awaitIdle();

        assertThat(events).hasSize(1);
        ResultEvent failure = events.get(0);
        String[] segments = failure.destination().split("/");
        Long failedMember = Long.valueOf(segments[segments.length - 2]);
        Long otherMember = failedMember.equals(1L) ? 2L : 1L;

        choiceHandler.execute(sessionId, failedMember, otherMember);
        choiceHandler.execute(sessionId, otherMember, failedMember);
        eventQueue.awaitIdle();

        assertThat(events).containsExactly(failure);
    }

    private ResultEvent failedEvent(Long memberId) {
        return new ResultEvent(BlindDateTopic.matchFailed(sessionId, memberId),
                Map.of("message", "매칭에 실패했습니다."));
    }

    private record ResultEvent(String destination, Object payload) {
    }
}
