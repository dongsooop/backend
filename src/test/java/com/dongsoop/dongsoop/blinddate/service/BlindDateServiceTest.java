package com.dongsoop.dongsoop.blinddate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateInfoRepositoryImpl;
import com.dongsoop.dongsoop.blinddate.repository.ParticipantInfoRepository;
import com.dongsoop.dongsoop.blinddate.repository.SessionInfoRepository;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlindDateService 단위 테스트")
class BlindDateServiceTest {
    @Mock
    private ParticipantInfoRepository participantInfoRepository;
    @Mock
    private BlindDateInfoRepositoryImpl blindDateInfoRepository;
    @Mock
    private BlindDateNotification blindDateNotification;
    @Mock
    private SessionInfoRepository sessionInfoRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private BlindDateTaskScheduler taskScheduler;
    @InjectMocks
    private BlindDateServiceImpl blindDateService;

    @Nested
    @DisplayName("isAvailable 메서드")
    class IsAvailableTest {
        @Test
        @DisplayName("과팅 운영 중이면 true 반환")
        void whenAvailable_thenReturnTrue() {
            given(blindDateInfoRepository.isAvailable()).willReturn(true);
            boolean result = blindDateService.isAvailable();
            assertThat(result).isTrue();
            verify(blindDateInfoRepository).isAvailable();
        }

        @Test
        @DisplayName("과팅 종료 상태면 false 반환")
        void whenNotAvailable_thenReturnFalse() {
            given(blindDateInfoRepository.isAvailable()).willReturn(false);
            boolean result = blindDateService.isAvailable();
            assertThat(result).isFalse();
            verify(blindDateInfoRepository).isAvailable();
        }
    }

    @Nested
    @DisplayName("startBlindDate 메서드")
    class StartBlindDateTest {
        @Test
        @DisplayName("과팅 시작 성공")
        void startBlindDate_Success() {
            LocalDateTime expiredDate = LocalDateTime.now().plusHours(1);
            StartBlindDateRequest request = new StartBlindDateRequest(expiredDate, 5);
            blindDateService.startBlindDate(request);
            verify(blindDateInfoRepository).start(5, expiredDate);
            verify(blindDateNotification).send();
        }
    }

    @Nested
    @DisplayName("broadcastJoinedCount 메서드")
    class BroadcastJoinedCountTest {
        @Test
        @DisplayName("세션 입장 인원 브로드캐스트")
        void broadcastJoinedCount() {
            String sessionId = "session-123";
            int count = 5;
            blindDateService.broadcastJoinedCount(sessionId, count);
            verify(messagingTemplate).convertAndSend(
                    BlindDateTopic.joined(sessionId),
                    java.util.Map.of("volunteer", count)
            );
        }
    }
}
