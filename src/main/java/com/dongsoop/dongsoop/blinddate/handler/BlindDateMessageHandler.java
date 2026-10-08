package com.dongsoop.dongsoop.blinddate.handler;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindDateMessageHandler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int RECORDED_MESSAGE_MAX_LENGTH = 1000;

    private final BlindDateParticipantStorage participantStorage;
    private final SimpMessagingTemplate messagingTemplate;
    private final BlindDateSessionStorage sessionStorage;

    /**
     * 메시지 브로드캐스트
     *
     * @param sessionId 대상 세션 id
     * @param senderId  발신자 id
     * @param message   발신 내용
     */
    public void execute(String sessionId, Long senderId, String message) {
        String senderName = participantStorage.getAnonymousName(senderId);
        if (senderName == null) {
            log.warn("Sender not found: senderId={}", senderId);
            return;
        }

        String messageId = UUID.randomUUID().toString();
        Map<String, Object> event = Map.of(
                "messageId", messageId,
                "message", message,
                "senderId", senderId,
                "senderName", senderName,
                "timestamp", System.currentTimeMillis()
        );

        // 방송보다 먼저 기록해, 받은 메시지를 바로 신고해도 찾을 수 있게 한다
        sessionStorage.recordMessage(sessionId,
                new BlindDateMessage(messageId, senderId, limitLength(message), LocalDateTime.now(KST)));

        messagingTemplate.convertAndSend(BlindDateTopic.message(sessionId), event);
    }

    // 신고 스냅샷도 1,000자로 잘리므로, 메모리 보관분만 같은 길이로 제한한다(방송은 원문)
    private static String limitLength(String message) {
        if (message.length() <= RECORDED_MESSAGE_MAX_LENGTH) {
            return message;
        }
        int end = RECORDED_MESSAGE_MAX_LENGTH;
        if (Character.isHighSurrogate(message.charAt(end - 1))) {
            end--;
        }
        return message.substring(0, end);
    }
}
