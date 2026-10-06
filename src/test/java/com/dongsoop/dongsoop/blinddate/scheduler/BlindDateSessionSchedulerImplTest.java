package com.dongsoop.dongsoop.blinddate.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.config.BlindDateMessageProvider;
import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("과팅 세션 스케줄러")
class BlindDateSessionSchedulerImplTest {

    private static final String SESSION_ID = "session-1";

    private final BlindDateParticipantStorage participantStorage = mock(BlindDateParticipantStorage.class);
    private final BlindDateSessionStorage sessionStorage = mock(BlindDateSessionStorage.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final BlindDateTaskScheduler taskScheduler = mock(BlindDateTaskScheduler.class);
    private final BlindDateChoiceHandler choiceHandler = mock(BlindDateChoiceHandler.class);
    private final BlindDateEventQueue eventQueue = spy(new BlindDateEventQueue());

    private BlindDateSessionSchedulerImpl scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BlindDateSessionSchedulerImpl(
                participantStorage,
                sessionStorage,
                mock(BlindDateMessageProvider.class),
                messagingTemplate,
                taskScheduler,
                eventQueue, choiceHandler);

        when(sessionStorage.isProcessing(SESSION_ID)).thenReturn(true);
        when(participantStorage.getParticipantsIdAndName(SESSION_ID))
                .thenReturn(Map.of(1L, "익명1", 2L, "익명2"));
    }

    @Test
    @DisplayName("참가자 목록 발행 전에 응답 대상을 고정하고 30초 미응답 타이머를 만든다")
    void opensChoicesBeforePublishingParticipants() {
        invokeScheduleSessionEnd();

        InOrder order = inOrder(participantStorage, eventQueue, messagingTemplate);
        order.verify(eventQueue).openChoices(eq(SESSION_ID), any(BooleanSupplier.class));
        order.verify(participantStorage).openChoices(SESSION_ID);
        order.verify(messagingTemplate).convertAndSend(
                eq(BlindDateTopic.participants(SESSION_ID)), any(Object.class));
        verify(taskScheduler).schedule(any(Runnable.class), eq(30_000L));
    }

    @Test
    @DisplayName("참가자 목록 발행 실패에도 열린 접수 상태를 유지한다")
    void opensChoicePeriodWhenPublishingParticipantsFails() {
        doThrow(new IllegalStateException("publish failed"))
                .when(messagingTemplate)
                .convertAndSend(anyString(), any(Object.class));

        assertThatCode(this::invokeScheduleSessionEnd).doesNotThrowAnyException();

        verify(eventQueue).openChoices(eq(SESSION_ID), any(BooleanSupplier.class));
        verify(taskScheduler).schedule(any(Runnable.class), eq(30_000L));
    }

    @AfterEach
    void tearDown() {
        eventQueue.shutdown();
    }

    @Test
    void terminatedSessionDoesNotReopenOrPublishParticipants() {
        when(sessionStorage.isProcessing(SESSION_ID)).thenReturn(false);
        invokeScheduleSessionEnd();
        verifyNoInteractions(participantStorage, messagingTemplate, taskScheduler);
    }

    @Test
    void timeoutTaskDelegatesToSharedChoiceFinalization() {
        invokeScheduleSessionEnd();
        ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(task.capture(), eq(30_000L));
        task.getValue().run();
        verify(choiceHandler).timeout(SESSION_ID);
    }

    @Test
    void failedTimerRegistrationFinalizesInsteadOfLeavingSessionOpen() {
        doThrow(new IllegalStateException("scheduler unavailable"))
                .when(taskScheduler).schedule(any(Runnable.class), eq(30_000L));
        assertThatCode(this::invokeScheduleSessionEnd).doesNotThrowAnyException();
        verify(choiceHandler).timeout(SESSION_ID);
    }

    private void invokeScheduleSessionEnd() {
        ReflectionTestUtils.invokeMethod(scheduler, "scheduleSessionEnd", SESSION_ID);
    }
}
