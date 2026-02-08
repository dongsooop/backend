package com.dongsoop.dongsoop.blinddate.service;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateInfoRepositoryImpl;
import com.dongsoop.dongsoop.blinddate.repository.ParticipantInfoRepository;
import com.dongsoop.dongsoop.blinddate.repository.SessionInfoRepository;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * BlindDate Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlindDateServiceImpl implements BlindDateService {

    private final ParticipantInfoRepository participantInfoRepository;
    private final BlindDateInfoRepositoryImpl blindDateInfoRepository;
    private final BlindDateNotification blindDateNotification;
    private final SessionInfoRepository sessionInfoRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final BlindDateTaskScheduler taskScheduler;

    /**
     * 과팅 운영 상태 확인
     */
    public boolean isAvailable() {
        return blindDateInfoRepository.isAvailable();
    }

    /**
     * 과팅 시작
     *
     * @param request 과팅 개최 시 필요한 세션별 정원 수, 종료 시간
     */
    public void startBlindDate(StartBlindDateRequest request) {
        log.info("Starting blind date: expiredDate={}, maxSessionMemberCount={}",
                request.getExpiredDate(), request.getMaxSessionMemberCount());

        try {
            // 과팅 시작
            blindDateInfoRepository.start(request.getMaxSessionMemberCount(), request.getExpiredDate());

            // 자동 종료 스케줄링 (TaskScheduler 사용)
            scheduleAutoClose(request.getExpiredDate());

            // 과팅 오픈 알림 전송
            blindDateNotification.send();

            log.info("[BlindDate] started successfully");
        } catch (Exception e) {
            log.error("[BlindDate] Failed to start", e);
            throw new RuntimeException("[BlindDate] Failed to start: " + e.getMessage(), e);
        }
    }

    /**
     * 자동 종료 스케줄링 (TaskScheduler 사용)
     *
     * @param expiredDate 종료 시간
     */
    private void scheduleAutoClose(LocalDateTime expiredDate) {
        try {
            // TaskScheduler에 과팅 종료 작업 등록
            taskScheduler.scheduleBlindDateEnd(expiredDate, () -> {
                log.info("Auto closing blind date at: {}", LocalDateTime.now());

                // 모든 세션 종료
                sessionInfoRepository.clear();

                // 모든 참가자 정보 삭제
                participantInfoRepository.clear();

                // 과팅 상태 종료
                blindDateInfoRepository.close();

                // TaskScheduler 정리
                taskScheduler.cleanupAllSessions();

                log.info("BlindDate closed successfully");
            });

            log.info("Scheduled auto close at: {}", expiredDate);
        } catch (Exception e) {
            log.error("Failed to schedule auto close", e);
            throw new RuntimeException("Failed to schedule auto close: " + e.getMessage(), e);
        }
    }

    /**
     * 사용자 입장 시 사용자 수 업데이트 소켓 이벤트 발행
     *
     * @param sessionId 대상 세션 id
     * @param count     세션 내 참여중인 사용자 수
     */
    public void broadcastJoinedCount(String sessionId, int count) {
        messagingTemplate.convertAndSend(
                BlindDateTopic.joined(sessionId),
                Map.of("volunteer", count)
        );
    }
}
