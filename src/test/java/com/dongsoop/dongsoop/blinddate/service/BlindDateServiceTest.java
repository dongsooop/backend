package com.dongsoop.dongsoop.blinddate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import com.dongsoop.dongsoop.blinddate.exception.BlindDateSessionNotFoundException;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
import java.time.LocalDateTime;
import java.util.List;
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
    @DisplayName("특정 세션 강제 종료")
    class CloseSessionTest {

        @Test
        @DisplayName("대기 세션을 닫으면 포인터를 비우고 세션을 종료하며, 참가자 기록을 지우고 종료를 알린다")
        void close_waitingSession_clearsPointerTerminatesAndReleasesParticipants() {
            String sessionId = "session-1";
            blindDateStorage.setPointer(sessionId);
            given(sessionStorage.getState(sessionId)).willReturn(SessionState.WAITING);
            given(participantStorage.findAllBySessionId(sessionId)).willReturn(List.of(
                    ParticipantInfo.create(sessionId, 2L, "s2", "익명1"),
                    ParticipantInfo.create(sessionId, 4L, "s4", "익명2")));

            blindDateService.closeSession(sessionId);

            ArgumentCaptor<Runnable> queued = ArgumentCaptor.forClass(Runnable.class);
            verify(eventQueue).submit(queued.capture());
            verify(sessionStorage, never()).terminate(sessionId);

            queued.getValue().run();

            assertThat(blindDateStorage.getPointer()).isNull();
            verify(sessionStorage).terminate(sessionId);
            verify(participantStorage).removeParticipant(2L);
            verify(participantStorage).removeParticipant(4L);
            verify(messagingTemplate).convertAndSendToUser("2", "/queue/blinddate/join", Map.of("state", "TERMINATED"));
            verify(messagingTemplate).convertAndSendToUser("4", "/queue/blinddate/join", Map.of("state", "TERMINATED"));
        }

        @Test
        @DisplayName("대기 중이 아닌 세션을 닫으면 포인터는 그대로 둔다")
        void close_otherSession_keepsPointer() {
            blindDateStorage.setPointer("waiting-session");
            given(sessionStorage.getState("running-session")).willReturn(SessionState.PROCESSING);

            blindDateService.closeSession("running-session");

            ArgumentCaptor<Runnable> queued = ArgumentCaptor.forClass(Runnable.class);
            verify(eventQueue).submit(queued.capture());
            queued.getValue().run();

            assertThat(blindDateStorage.getPointer()).isEqualTo("waiting-session");
            verify(sessionStorage).terminate("running-session");
        }

        @Test
        @DisplayName("없는 세션이면 404 예외를 던지고 아무것도 처리하지 않는다")
        void close_unknownSession_throwsNotFound() {
            assertThatThrownBy(() -> blindDateService.closeSession("unknown"))
                    .isInstanceOf(BlindDateSessionNotFoundException.class);

            verify(eventQueue, never()).submit(org.mockito.ArgumentMatchers.any());
        }
    }
}
