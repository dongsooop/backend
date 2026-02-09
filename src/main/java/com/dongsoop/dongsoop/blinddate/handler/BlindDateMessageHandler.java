package com.dongsoop.dongsoop.blinddate.handler;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlindDateMessageHandler {

    private final BlindDateParticipantStorage participantStorage;
    private final SimpMessagingTemplate messagingTemplate;

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

        Map<String, Object> event = Map.of(
                "message", message,
                "senderId", senderId,
                "senderName", senderName,
                "timestamp", System.currentTimeMillis()
        );

        messagingTemplate.convertAndSend(BlindDateTopic.message(sessionId), event);
    }
}
