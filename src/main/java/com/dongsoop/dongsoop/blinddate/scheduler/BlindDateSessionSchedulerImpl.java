package com.dongsoop.dongsoop.blinddate.scheduler;

import com.dongsoop.dongsoop.blinddate.config.BlindDateMessageProvider;
import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindDateSessionSchedulerImpl implements BlindDateSessionScheduler {

    // 시간 설정
    private static final long SUBSCRIPTION_DELAY = 1000;
    private static final long START_MESSAGE_DELAY = 2000;
    private static final long MESSAGE_WAITING_TIME = 4000;
    private static final long CHATTING_TIME = 3 * 60 * 1000; // 3분
    private static final long CHOICE_TIME = 10 * 1000;

    private final BlindDateParticipantStorage participantStorage;
    private final BlindDateSessionStorage sessionStorage;
    private final BlindDateMessageProvider messageProvider;
    private final SimpMessagingTemplate messagingTemplate;
    private final BlindDateTaskScheduler taskScheduler;

    @Value("${blinddate.event-message-amount}")
    private int eventMessageAmount;

    /**
     * 세션 시작
     *
     * @param sessionId 시작할 세션 id
     */
    public void start(String sessionId) {
        if (this.sessionStorage.getState(sessionId) != SessionState.WAITING) {
            log.warn("[BlindDate] Starting session is not waiting");
            return;
        }

        try {
            log.info("Session starting: {}", sessionId);

            // 세션 상태 변경 - PROCESSING
            sessionStorage.start(sessionId);

            // 클라이언트가 세션 토픽을 구독할 시간 확보 (START 이벤트 전송 전에 대기)
            Thread.sleep(SUBSCRIPTION_DELAY);

            // START 이벤트 전송
            sendStartEvent(sessionId);

            // START 이벤트 수신 후 추가 처리 시간 확보
            Thread.sleep(SUBSCRIPTION_DELAY);

            // FREEZE (채팅 비활성화)
            sendFreeze(sessionId);

            // 시작 메시지 순차 전송 (2초 간격) - 메인 스레드에서 처리
            List<String> startMessages = messageProvider.getStartMessages();
            for (String msg : startMessages) {
                sendSystemMessage(sessionId, msg);
                Thread.sleep(START_MESSAGE_DELAY);
            }

            // 이벤트 메시지는 TaskScheduler에 등록하여 스케줄링
            scheduleEventMessages(sessionId);

        } catch (InterruptedException e) {
            log.error("Session interrupted: {}", sessionId, e);
            Thread.currentThread().interrupt();
            this.sessionStorage.terminate(sessionId);
        } catch (Exception e) {
            log.error("Error in session: {}", sessionId, e);
            this.sessionStorage.terminate(sessionId);
        }
    }

    /**
     * 이벤트 메시지를 TaskScheduler에 등록 (3분 간격)
     *
     * @param sessionId 대상 세션 id
     */
    private void scheduleEventMessages(String sessionId) {
        List<String> eventMessages = messageProvider.getRandomEventMessages(this.eventMessageAmount);

        // 전송하려는 이벤트 메시지 수에 따라 작업 스케줄러 등록
        sendEventMessage(0, sessionId, eventMessages);
    }

    /**
     * 이벤트 메시지 발행
     *
     * @param index         발행할 이벤트 메시의 번호
     * @param sessionId     이벤트 메시지를 발행할 세션 id
     * @param eventMessages 이벤트 메시지 목록
     */
    private void sendEventMessage(int index, String sessionId, List<String> eventMessages) {
        if (this.sessionStorage.getState(sessionId) == null) {
            log.info("[BlindDate] Session already terminated, skipping event message {} for session {}", index + 1,
                    sessionId);
            return;
        }

        try {
            log.info("[BlindDate] Sending event message {} for session {}", index + 1, sessionId);

            // FREEZE (채팅 비활성화)
            sendFreeze(sessionId);

            // 이벤트 메시지 전송
            sendSystemMessage(sessionId, eventMessages.get(index));

            // MESSAGE_WAITING_TIME 후 채팅 활성화
            taskScheduler.schedule(() -> scheduledNextEventMessage(index, sessionId, eventMessages),
                    MESSAGE_WAITING_TIME);
        } catch (Exception e) {
            log.error("Error sending event message {} for session {}", index, sessionId, e);
            sessionStorage.terminate(sessionId);
        }
    }

    private void scheduledNextEventMessage(int index, String sessionId, List<String> eventMessages) {
        try {
            // 채팅 녹이기
            sendThaw(sessionId);

            // 마지막 이벤트 메시지 후 참가자 목록 전송 및 세션 종료
            if (index == eventMessages.size() - 1) {
                taskScheduler.schedule(() -> scheduleSessionEnd(sessionId), CHATTING_TIME);
                return;
            }

            // 다음 메시지 스케줄링(스케줄링 실행 시 다음 스케줄링 등록)
            taskScheduler.schedule(() -> sendEventMessage(index + 1, sessionId, eventMessages), CHATTING_TIME);

        } catch (Exception e) {
            // 위 코드에서 예외 발생 시 세션 종료
            log.error("[BlindDate] Error in scheduled thaw/next for session {}", sessionId, e);
            sessionStorage.terminate(sessionId);
        }
    }

    /**
     * 세션 종료 스케줄링
     *
     * @param sessionId 종료할 세션 id
     */
    private void scheduleSessionEnd(String sessionId) {
        log.info("[BlindDate] Sending participants list for session: {}", sessionId);

        try {
            // 사랑의 작대기를 위해 사용자에게 사용자 목록 이벤트 발행
            this.sendParticipantsList(sessionId);
        } catch (Exception e) {
            // 세션 종료처리를 위해 로그만 남기고 계속 진행
            log.error("[BlindDate] Error sending participants list for session: {}", sessionId, e);
        }

        taskScheduler.schedule(() -> finalizeSession(sessionId), CHOICE_TIME);
    }

    /**
     * 세션 최종 종료 처리
     *
     * @param sessionId 종료할 세션 id
     */
    private void finalizeSession(String sessionId) {
        try {
            if (this.sessionStorage.getState(sessionId) == null) {
                log.warn("[BlindDate] Session already terminated: {}", sessionId);
                return;
            }

            log.info("[BlindDate] Finalizing session: {}", sessionId);

            // 매칭 실패자에게 FAILED 이벤트
            sendFailedToUnmatched(sessionId);

            // 세션 종료
            sessionStorage.terminate(sessionId);

            // 회원 정보는 재 접속 방지를 위해 제거하지 않음
            // participantStorage.clearSession(sessionId);

            log.info("Session ended: {}", sessionId);
        } catch (Exception e) {
            log.error("Error finalizing session: {}", sessionId, e);
        }
    }

    /**
     * 시작 이벤트 발행
     *
     * @param sessionId 시작 세션 id
     */
    private void sendStartEvent(String sessionId) {
        String topic = BlindDateTopic.sessionStart(sessionId);
        Map<String, Object> payload = Map.of("sessionId", sessionId);

        try {
            messagingTemplate.convertAndSend(topic, payload);
            log.info("[{}] START event sent for session: {}", System.currentTimeMillis(), sessionId);
        } catch (Exception e) {
            log.error("Failed to send START event", e);
            throw e;
        }
    }

    /**
     * 매치 실패 이벤트 발행
     *
     * @param sessionId 대상 세션 id
     */
    private void sendFailedToUnmatched(String sessionId) {
        Set<Long> notMatched = participantStorage.getNotMatched(sessionId);
        // 모두 매치되었다면 이벤트를 발행하지 않음
        if (notMatched.isEmpty()) {
            return;
        }

        // 매치되지 않은 사람이 존재하는 경우 매칭 실패 이벤트 발행
        for (Long memberId : notMatched) {
            messagingTemplate.convertAndSend(
                    BlindDateTopic.matchFailed(sessionId, memberId),
                    Map.of("message", "매칭에 실패했습니다.")
            );
        }
    }

    /**
     * 채팅 얼리기 이벤트 발행
     *
     * @param sessionId 대상 세션 id
     */
    private void sendFreeze(String sessionId) {
        messagingTemplate.convertAndSend(BlindDateTopic.freeze(sessionId), Map.of("type", "FREEZE"));
    }

    /**
     * 채팅 녹이기 이벤트 발행
     *
     * @param sessionId 대상 세션 id
     */
    private void sendThaw(String sessionId) {
        messagingTemplate.convertAndSend(BlindDateTopic.thaw(sessionId), Map.of("type", "THAW"));
    }

    /**
     * 시스템 메시지 전송
     *
     * @param sessionId 대상 세션 id
     * @param message   메시지
     */
    private void sendSystemMessage(String sessionId, String message) {
        Map<String, Object> event = Map.of(
                "message", message,
                "senderId", 0L,
                "senderName", messageProvider.getSessionManagerName(),
                "timestamp", System.currentTimeMillis()
        );
        messagingTemplate.convertAndSend(BlindDateTopic.system(sessionId), event);
    }

    /**
     * 사용자 목록 전송 이벤트 발행(사랑의 작대기를 위한)
     *
     * @param sessionId 대상 세션 id
     */
    private void sendParticipantsList(String sessionId) {
        Map<Long, String> participants = participantStorage.getParticipantsIdAndName(sessionId);
        messagingTemplate.convertAndSend(
                BlindDateTopic.participants(sessionId),
                Map.of("participants", participants)
        );
    }
}
