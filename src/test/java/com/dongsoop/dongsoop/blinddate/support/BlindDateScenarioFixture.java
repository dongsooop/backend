package com.dongsoop.dongsoop.blinddate.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.config.BlindDateMessageProvider;
import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateConnectHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateDisconnectHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateMessageHandler;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateSessionScheduler;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateSessionSchedulerImpl;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
import com.dongsoop.dongsoop.blinddate.service.BlindDateServiceImpl;
import com.dongsoop.dongsoop.blinddate.service.BlindDateSessionServiceImpl;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** 실제 상태 저장소·서비스·핸들러를 연결하고 시간 예약과 외부 I/O를 제어한다. */
public final class BlindDateScenarioFixture implements AutoCloseable {
    public final BlindDateEventQueue queue = new BlindDateEventQueue();
    public final BlindDateParticipantStorageImpl participants =
            new BlindDateParticipantStorageImpl();
    public final BlindDateSessionStorageImpl sessions = new BlindDateSessionStorageImpl();
    public final BlindDateStorageImpl operation = new BlindDateStorageImpl();
    public final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    public final ChatRoomService rooms = mock(ChatRoomService.class);
    public final BlindDateTaskScheduler tasks = mock(BlindDateTaskScheduler.class);
    public final BlindDateNotification notification = mock(BlindDateNotification.class);
    public final BlindDateSessionScheduler sessionStarter = mock(BlindDateSessionScheduler.class);
    public final List<Event> events = new CopyOnWriteArrayList<>();
    public final List<List<Long>> createdPairs = new CopyOnWriteArrayList<>();
    public final BlockingQueue<Runnable> timers = new LinkedBlockingQueue<>();
    public final BlindDateServiceImpl service;
    public final BlindDateConnectHandler connect;
    public final BlindDateDisconnectHandler disconnect;
    public final BlindDateChoiceHandler choice;
    public final BlindDateMessageHandler message;
    private final BlindDateSessionSchedulerImpl scheduler;
    private Runnable operatingEnd;

    public BlindDateScenarioFixture(int capacity) {
        doAnswer(
                        call -> {
                            events.add(new Event(call.getArgument(0), call.getArgument(1)));
                            return null;
                        })
                .when(messaging)
                .convertAndSend(anyString(), any(Object.class));
        doAnswer(
                        call -> {
                            timers.add(call.getArgument(0));
                            return null;
                        })
                .when(tasks)
                .schedule(any(Runnable.class), anyLong());
        doAnswer(
                        call -> {
                            operatingEnd = call.getArgument(0);
                            return null;
                        })
                .when(tasks)
                .schedule(any(Runnable.class), any(LocalDateTime.class));
        var number = new AtomicInteger();
        when(rooms.createOneToOneChatRoom(anyLong(), anyLong(), anyString()))
                .thenAnswer(
                        call -> {
                            createdPairs.add(List.of(call.getArgument(0), call.getArgument(1)));
                            return ChatRoom.builder()
                                    .roomId("room-" + number.incrementAndGet())
                                    .build();
                        });
        service =
                new BlindDateServiceImpl(
                        participants, operation, notification, sessions, messaging, tasks, queue);
        disconnect = new BlindDateDisconnectHandler(participants, sessions, service, queue);
        connect =
                new BlindDateConnectHandler(
                        participants,
                        operation,
                        sessions,
                        service,
                        new BlindDateSessionServiceImpl(participants, operation),
                        sessionStarter,
                        messaging,
                        queue,
                        disconnect);
        choice = new BlindDateChoiceHandler(participants, sessions, messaging, rooms, queue);
        message = new BlindDateMessageHandler(participants, messaging);
        var messages = mock(BlindDateMessageProvider.class);
        when(messages.getStartMessages()).thenReturn(List.of());
        when(messages.getRandomEventMessages(1)).thenReturn(List.of("event"));
        when(messages.getSessionManagerName()).thenReturn("manager");
        scheduler =
                new BlindDateSessionSchedulerImpl(
                        participants, sessions, messages, messaging, tasks, queue);
        ReflectionTestUtils.setField(scheduler, "eventMessageAmount", 1);
        service.startBlindDate(
                new StartBlindDateRequest(LocalDateTime.now().plusHours(1), capacity));
    }

    public Map<String, Object> join(long member, String socket) {
        Map<String, Object> attributes = new HashMap<>();
        connect.execute(socket, member, attributes);
        queue.awaitIdle();
        return attributes;
    }

    public String joinMembers(int count) {
        String session = null;
        for (long member = 1; member <= count; member++) {
            session = (String) join(member, "socket-" + member).get("sessionId");
        }
        return session;
    }

    public void openChoices(String session) {
        // 세션 시작 연결 자체는 기존 FullFlow 테스트에서 검증한다.
        scheduler.start(session);
        runNextTimer();
        runNextTimer();
    }

    public void runNextTimer() {
        try {
            Runnable task = timers.poll(5, TimeUnit.SECONDS);
            assertThat(task).as("예약 작업이 있어야 한다").isNotNull();
            task.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    public void finishChoices() {
        runNextTimer();
        queue.awaitIdle();
    }

    public void endOperation() {
        operatingEnd.run();
    }

    public List<Event> results() {
        return events.stream()
                .filter(
                        event ->
                                event.destination().endsWith("/chatroom")
                                        || event.destination().endsWith("/failed"))
                .toList();
    }

    public List<Long> recipients(String suffix) {
        return results().stream()
                .filter(event -> event.destination().endsWith(suffix))
                .map(
                        event -> {
                            String[] parts = event.destination().split("/");
                            return Long.valueOf(parts[parts.length - 2]);
                        })
                .toList();
    }

    public record Event(String destination, Object payload) {}

    @Override
    public void close() {
        queue.shutdown();
    }
}
