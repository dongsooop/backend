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
import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateConnectHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateDisconnectHandler;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateSessionSchedulerImpl;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
import com.dongsoop.dongsoop.blinddate.service.BlindDateServiceImpl;
import com.dongsoop.dongsoop.blinddate.service.BlindDateSessionServiceImpl;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("과팅 시작부터 매칭 결과까지 통합 흐름")
class BlindDateFullFlowIntegrationTest {

    private final BlindDateEventQueue eventQueue = new BlindDateEventQueue();

    @AfterEach
    void tearDown() {
        eventQueue.shutdown();
    }

    @Test
    @DisplayName("일부 매칭 성공을 먼저 알리고 나머지 참가자에게 실패를 알린 뒤 세션을 종료한다")
    void sendsSuccessThenFailureAndTerminatesSession() throws Exception {
        var blindDateStorage = new BlindDateStorageImpl();
        var participantStorage = new BlindDateParticipantStorageImpl();
        var sessionStorage = new BlindDateSessionStorageImpl();
        var taskScheduler = new ControllableTaskScheduler();
        var resultEvents = new CopyOnWriteArrayList<ResultEvent>();
        var messaging = resultCapturingMessagingTemplate(resultEvents);
        var notification = mock(BlindDateNotification.class);
        var chatRoomService = mock(ChatRoomService.class);
        when(chatRoomService.createOneToOneChatRoom(anyLong(), anyLong(), anyString()))
                .thenReturn(ChatRoom.builder().roomId("room-1").build());

        var service = new BlindDateServiceImpl(
                participantStorage,
                blindDateStorage,
                notification,
                sessionStorage,
                messaging,
                taskScheduler,
                eventQueue
        );
        var sessionService = new BlindDateSessionServiceImpl(participantStorage, blindDateStorage);
        var messages = mock(BlindDateMessageProvider.class);
        when(messages.getStartMessages()).thenReturn(List.of());
        when(messages.getRandomEventMessages(1)).thenReturn(List.of("event"));
        when(messages.getSessionManagerName()).thenReturn("manager");

        var sessionScheduler = new BlindDateSessionSchedulerImpl(
                participantStorage,
                sessionStorage,
                messages,
                messaging,
                taskScheduler,
                eventQueue
        );
        ReflectionTestUtils.setField(sessionScheduler, "eventMessageAmount", 1);

        var disconnectHandler = new BlindDateDisconnectHandler(
                participantStorage,
                sessionStorage,
                service,
                eventQueue
        );
        var connectHandler = new BlindDateConnectHandler(
                participantStorage,
                blindDateStorage,
                sessionStorage,
                service,
                sessionService,
                sessionScheduler,
                messaging,
                eventQueue,
                disconnectHandler
        );
        var choiceHandler = new BlindDateChoiceHandler(
                participantStorage,
                sessionStorage,
                messaging,
                chatRoomService,
                eventQueue
        );

        service.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusHours(1), 3));

        Map<String, Object> first = new HashMap<>();
        Map<String, Object> second = new HashMap<>();
        Map<String, Object> third = new HashMap<>();
        connectHandler.execute("socket-1", 1L, first);
        connectHandler.execute("socket-2", 2L, second);
        connectHandler.execute("socket-3", 3L, third);
        eventQueue.awaitIdle();

        String sessionId = (String) first.get("sessionId");
        assertThat(second.get("sessionId")).isEqualTo(sessionId);
        assertThat(third.get("sessionId")).isEqualTo(sessionId);
        assertThat(blindDateStorage.getPointer()).isEqualTo(sessionId);
        assertThat(sessionStorage.getState(sessionId)).isEqualTo(SessionState.PROCESSING);
        assertThat(participantStorage.findAllBySessionId(sessionId)).hasSize(3);
        assertParticipantConnection(participantStorage, sessionId, 1L, "socket-1");
        assertParticipantConnection(participantStorage, sessionId, 2L, "socket-2");
        assertParticipantConnection(participantStorage, sessionId, 3L, "socket-3");

        // 실제 세션 스케줄러가 등록한 대화 단계 종료와 선택 단계 시작 작업을 순서대로 실행한다.
        taskScheduler.runNext();
        taskScheduler.runNext();

        choiceHandler.execute(sessionId, 1L, 2L);
        choiceHandler.execute(sessionId, 2L, 1L);

        // 선택 마감 작업은 앞서 접수된 성공 처리 뒤에 최종 결과 처리를 배치한다.
        taskScheduler.runNext();
        eventQueue.awaitIdle();

        assertThat(resultEvents).containsExactly(
                chatRoomCreated(sessionId, 2L),
                chatRoomCreated(sessionId, 1L),
                matchFailed(sessionId, 3L)
        );
        assertThat(sessionStorage.getState(sessionId)).isNull();
    }

    private void assertParticipantConnection(
            BlindDateParticipantStorageImpl participantStorage,
            String sessionId,
            Long memberId,
            String socketId
    ) {
        var byMember = participantStorage.getByMemberId(memberId);
        var bySocket = participantStorage.getBySocketId(socketId);

        assertThat(byMember).isNotNull();
        assertThat(bySocket).isSameAs(byMember);
        assertThat(byMember.getSessionId()).isEqualTo(sessionId);
        assertThat(byMember.getMemberId()).isEqualTo(memberId);
        assertThat(byMember.getSocketIds()).containsExactly(socketId);
    }

    private SimpMessagingTemplate resultCapturingMessagingTemplate(List<ResultEvent> resultEvents) {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        doAnswer(invocation -> {
            String destination = invocation.getArgument(0);
            if (destination.endsWith("/chatroom") || destination.endsWith("/failed")) {
                resultEvents.add(new ResultEvent(destination, invocation.getArgument(1)));
            }
            return null;
        }).when(messaging).convertAndSend(anyString(), any(Object.class));
        return messaging;
    }

    private ResultEvent chatRoomCreated(String sessionId, Long memberId) {
        return new ResultEvent(
                BlindDateTopic.chatRoomCreated(sessionId, memberId),
                Map.of("chatRoomId", "room-1")
        );
    }

    private ResultEvent matchFailed(String sessionId, Long memberId) {
        return new ResultEvent(
                BlindDateTopic.matchFailed(sessionId, memberId),
                Map.of("message", "매칭에 실패했습니다.")
        );
    }

    private record ResultEvent(String destination, Object payload) {
    }

    private static final class ControllableTaskScheduler extends BlindDateTaskScheduler {

        private final BlockingQueue<Runnable> sessionTasks = new LinkedBlockingQueue<>();

        @Override
        public void schedule(Runnable task, long delay) {
            sessionTasks.add(task);
        }

        @Override
        public void schedule(Runnable cleanupTask, LocalDateTime endTime) {
            // 과팅 운영 종료 예약은 이 세션 결과 흐름에서 실행하지 않는다.
        }

        @Override
        public void cleanupAllSessions() {
            sessionTasks.clear();
        }

        private void runNext() throws InterruptedException {
            Runnable task = sessionTasks.poll(5, TimeUnit.SECONDS);
            assertThat(task).as("다음 과팅 세션 작업이 등록되어야 한다").isNotNull();
            task.run();
        }
    }
}
