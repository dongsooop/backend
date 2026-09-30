package com.dongsoop.dongsoop.blinddate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlindDateService 단위 테스트")
class BlindDateServiceTest {

    @Mock
    private BlindDateParticipantStorage participantStorage;
    @Mock
    private BlindDateNotification blindDateNotification;
    @Mock
    private BlindDateSessionStorage sessionStorage;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private BlindDateTaskScheduler taskScheduler;
    @Mock
    private BlindDateEventQueue eventQueue;

    private BlindDateStorage blindDateStorage;
    private BlindDateServiceImpl blindDateService;

    @BeforeEach
    void setUp() {
        blindDateStorage = new BlindDateStorageImpl();
        blindDateService = new BlindDateServiceImpl(
                participantStorage,
                blindDateStorage,
                blindDateNotification,
                sessionStorage,
                messagingTemplate,
                taskScheduler,
                eventQueue
        );
    }

    @Nested
    @DisplayName("과팅 운영 상태 조회")
    class IsAvailableTest {

        @Test
        @DisplayName("시작 전에는 운영 중이 아니다")
        void beforeStart_isNotAvailable() {
            assertThat(blindDateService.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("과팅을 시작하면 운영 중 상태가 된다")
        void afterStart_isAvailable() {
            StartBlindDateRequest request = new StartBlindDateRequest(
                    LocalDateTime.now().plusHours(1),
                    5
            );

            blindDateService.startBlindDate(request);

            assertThat(blindDateService.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("과팅 시작")
    class StartBlindDateTest {

        @Test
        @DisplayName("오픈 알림 전송에 실패해도 과팅은 정상적으로 시작된다")
        void notificationFailure_doesNotPreventStart() {
            StartBlindDateRequest request = new StartBlindDateRequest(
                    LocalDateTime.now().plusHours(1),
                    5
            );
            doThrow(new RuntimeException("notification failed"))
                    .when(blindDateNotification)
                    .send();

            blindDateService.startBlindDate(request);

            assertThat(blindDateService.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("자동 종료 예약에 실패하면 시작 상태를 롤백한다")
        void schedulingFailure_rollsBackStart() {
            LocalDateTime expiredDate = LocalDateTime.now().plusHours(1);
            StartBlindDateRequest request = new StartBlindDateRequest(expiredDate, 5);
            doThrow(new IllegalStateException("schedule failed"))
                    .when(taskScheduler)
                    .schedule(org.mockito.ArgumentMatchers.any(Runnable.class),
                            org.mockito.ArgumentMatchers.eq(expiredDate));

            assertThatThrownBy(() -> blindDateService.startBlindDate(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to scheduled close");
            assertThat(blindDateService.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("세션 입장 인원 브로드캐스트")
    class BroadcastJoinedCountTest {

        @Test
        @DisplayName("구독자에게 현재 입장 인원을 전달한다")
        void broadcastJoinedCount() {
            String sessionId = "session-123";
            int count = 5;

            blindDateService.broadcastJoinedCount(sessionId, count);

            verify(messagingTemplate).convertAndSend(
                    BlindDateTopic.joined(sessionId),
                    Map.of("volunteer", count)
            );
        }
    }

    @Nested
    @DisplayName("과팅 초기화")
    class ResetBlindDateTest {

        @Test
        @DisplayName("예약 작업을 취소하고 과팅을 닫은 뒤, 큐에서 세션·참가자 기록을 비운다")
        void reset_cancelsTasksClosesAndClearsStorages() {
            blindDateService.startBlindDate(new StartBlindDateRequest(LocalDateTime.now().plusHours(1), 5));

            blindDateService.resetBlindDate();

            verify(taskScheduler).cleanupAllSessions();
            assertThat(blindDateService.isAvailable()).isFalse();

            ArgumentCaptor<Runnable> queued = ArgumentCaptor.forClass(Runnable.class);
            verify(eventQueue).submit(queued.capture());
            verify(sessionStorage, never()).clear();

            queued.getValue().run();

            verify(sessionStorage).clear();
            verify(participantStorage).clear();
        }
    }
}
