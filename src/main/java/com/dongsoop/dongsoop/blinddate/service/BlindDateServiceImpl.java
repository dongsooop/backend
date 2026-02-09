package com.dongsoop.dongsoop.blinddate.service;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorage;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
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

    private final BlindDateParticipantStorage participantStorage;
    private final BlindDateStorage blindDateStorage;
    private final BlindDateNotification blindDateNotification;
    private final BlindDateSessionStorage sessionStorage;
    private final SimpMessagingTemplate messagingTemplate;
    private final BlindDateTaskScheduler taskScheduler;

    /**
     * 과팅 운영 상태 확인
     */
    public boolean isAvailable() {
        return blindDateStorage.isAvailable();
    }

    /**
     * 과팅 시작
     *
     * @param request 과팅 개최 시 필요한 세션별 정원 수, 종료 시간
     */
    public void startBlindDate(StartBlindDateRequest request) {
        log.info("Starting blind date: expiredDate={}, maxSessionMemberCount={}",
                request.getExpiredDate(), request.getMaxSessionMemberCount());

        // 과팅 시작을 위한 데이터 초기화
        blindDateStorage.start(request.getMaxSessionMemberCount(), request.getExpiredDate());

        try {
            // 자동 종료 스케줄링 (TaskScheduler 사용)
            // TaskScheduler에 과팅 종료 작업 등록
            taskScheduler.schedule(this::scheduleAutoClose, request.getExpiredDate());
        } catch (Exception e) {
            log.error("[BlindDate] Failed to scheduled close", e);
            throw new RuntimeException("[BlindDate] Failed to scheduled close: " + e.getMessage(), e);
        }

        try {
            // 과팅 오픈 알림 전송
            blindDateNotification.send();
        } catch (Exception e) {
            log.warn("[BlindDate] Cannot send notification message: {}", e.getMessage(), e);
        }
    }

    /**
     * 자동 종료 스케줄링 (TaskScheduler 사용)
     */
    private void scheduleAutoClose() {
        // 과팅 상태 종료
        blindDateStorage.close();

        // 모든 세션 정보 삭제
        sessionStorage.clear();

        // 모든 참가자 정보 삭제
        participantStorage.clear();

        // TaskScheduler 정리
        taskScheduler.cleanupAllSessions();
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
