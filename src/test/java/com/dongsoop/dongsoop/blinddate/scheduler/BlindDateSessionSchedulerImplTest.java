package com.dongsoop.dongsoop.blinddate.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.config.BlindDateMessageProvider;
import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("과팅 세션 스케줄러")
class BlindDateSessionSchedulerImplTest {

    private static final String SESSION_ID = "session-1";
    private static final long CHOICE_TIME = 10_000L;

    private final BlindDateParticipantStorage participantStorage = mock(BlindDateParticipantStorage.class);
    private final BlindDateSessionStorage sessionStorage = mock(BlindDateSessionStorage.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final BlindDateTaskScheduler taskScheduler = mock(BlindDateTaskScheduler.class);
    private final BlindDateEventQueue eventQueue = mock(BlindDateEventQueue.class);

    private BlindDateSessionSchedulerImpl scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BlindDateSessionSchedulerImpl(
                participantStorage,
                sessionStorage,
                mock(BlindDateMessageProvider.class),
                messagingTemplate,
                taskScheduler,
                eventQueue);

        when(sessionStorage.isProcessing(SESSION_ID)).thenReturn(true);
        when(participantStorage.getParticipantsIdAndName(SESSION_ID))
                .thenReturn(Map.of(1L, "익명1", 2L, "익명2"));
    }

    @Test
    @DisplayName("참가자 목록 이벤트 발행 후 선택 창을 열고 10초 뒤 마감한다")
    void opensChoicePeriodAfterPublishingParticipants() {
        invokeScheduleSessionEnd();

        InOrder order = inOrder(messagingTemplate, eventQueue, taskScheduler);
        order.verify(messagingTemplate).convertAndSend(
                eq(BlindDateTopic.participants(SESSION_ID)), any(Object.class));
        order.verify(eventQueue).openChoices(SESSION_ID, CHOICE_TIME);
        order.verify(taskScheduler).schedule(any(Runnable.class), eq(CHOICE_TIME));
    }

    @Test
    @DisplayName("참가자 목록 이벤트 발행에 실패해도 선택 창과 마감 타이머를 연다")
    void opensChoicePeriodWhenPublishingParticipantsFails() {
        doThrow(new IllegalStateException("publish failed"))
                .when(messagingTemplate)
                .convertAndSend(anyString(), any(Object.class));

        assertThatCode(this::invokeScheduleSessionEnd).doesNotThrowAnyException();

        verify(eventQueue).openChoices(SESSION_ID, CHOICE_TIME);
        verify(taskScheduler).schedule(any(Runnable.class), eq(CHOICE_TIME));
    }

    private void invokeScheduleSessionEnd() {
        ReflectionTestUtils.invokeMethod(scheduler, "scheduleSessionEnd", SESSION_ID);
    }
}
