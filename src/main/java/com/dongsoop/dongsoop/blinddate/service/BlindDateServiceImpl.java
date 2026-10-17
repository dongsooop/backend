package com.dongsoop.dongsoop.blinddate.service;

import com.dongsoop.dongsoop.blinddate.config.BlindDateMessageProvider;
import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.executor.BlindDateEventQueue;
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

    private static final int BLINDDATE_CLOSE_DELAY = 30 * 60 * 1000; // 30분
    private static final String JOIN_DESTINATION = "/queue/blinddate/join";

    private final BlindDateParticipantStorage participantStorage;
    private final BlindDateStorage blindDateStorage;
    private final BlindDateNotification blindDateNotification;
    private final BlindDateSessionStorage sessionStorage;
    private final SimpMessagingTemplate messagingTemplate;
    private final BlindDateTaskScheduler taskScheduler;
    private final BlindDateEventQueue eventQueue;
    private final BlindDateMessageProvider messageProvider;

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
            blindDateStorage.close();
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
     * 과팅 운영 상태와 자동 종료 예약은 유지하면서 참가자 입장 기록을 초기화한다.
     */
    @Override
    public void resetParticipants() {
        eventQueue.submitCleanup(() -> {
            blindDateStorage.setPointer(null);
            sessionStorage.clear();
            participantStorage.clear();
        });
    }

    /**
     * 자동 종료 스케줄링 (TaskScheduler 사용)
     */
    private void scheduleAutoClose() {
        blindDateStorage.stop();

        // 과팅 종료 상태 설정 후 BLINDDATE_CLOSE_DELAY만큼 지난 뒤 데이터 초기화
        taskScheduler.schedule(() -> {
            // 과팅 상태 종료
            blindDateStorage.close();

            // 세션/참가자 데이터 정리는 큐를 통해 순서대로 처리
            eventQueue.submitCleanup(() -> {
                // 모든 세션 정보 삭제
                sessionStorage.clear();

                // 모든 참가자 정보 삭제
                participantStorage.clear();
            });

            // TaskScheduler 정리
            taskScheduler.cleanupAllSessions();
        }, BLINDDATE_CLOSE_DELAY);
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

    /**
     * 참가자 강퇴. 세션이 이미 끝났으면 알림 없이 재입장만 막고, 전체 초기화로 참가 기록이 없으면 아무것도 하지 않는다.
     *
     * @param sessionId 강퇴할 세션 id
     * @param memberId  강퇴 대상 회원 id
     */
    @Override
    public void kickParticipant(String sessionId, Long memberId) {
        eventQueue.submit(() -> {
            ParticipantInfo kicked = participantStorage.kick(sessionId, memberId);
            if (kicked == null || sessionStorage.getState(sessionId) == null) {
                return;
            }

            try {
                messagingTemplate.convertAndSendToUser(memberId.toString(), JOIN_DESTINATION,
                        Map.of("state", "KICKED"));
            } catch (Exception e) {
                log.error("Failed to send KICKED event: memberId={}", memberId, e);
            }
            messagingTemplate.convertAndSend(BlindDateTopic.system(sessionId), Map.of(
                    "message", kicked.getAnonymousName() + "님이 퇴장당했습니다.",
                    "senderId", 0L,
                    "senderName", messageProvider.getSessionManagerName(),
                    "timestamp", System.currentTimeMillis()
            ));
            if (sessionStorage.isWaiting(sessionId)) {
                broadcastJoinedCount(sessionId, participantStorage.findAllBySessionId(sessionId).size());
            }
        });
    }
}
