package com.dongsoop.dongsoop.blinddate.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class BlindDateMessageHandlerTest {

    private final BlindDateSessionStorageImpl sessions = new BlindDateSessionStorageImpl();
    private final BlindDateParticipantStorageImpl participants = new BlindDateParticipantStorageImpl();
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final BlindDateMessageHandler handler =
            new BlindDateMessageHandler(participants, messagingTemplate, sessions);

    @Test
    @DisplayName("메시지에 ID를 붙여 세션에 기록하고, 기존 필드에 같은 messageId만 더해 방송한다")
    @SuppressWarnings("unchecked")
    void execute_RecordsAndBroadcastsWithSameId() {
        String sessionId = sessions.create().getSessionId();
        participants.addParticipant(sessionId, 2L, "socket2");

        handler.execute(sessionId, 2L, "안녕");

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq(BlindDateTopic.message(sessionId)), event.capture());
        Map<String, Object> payload = (Map<String, Object>) event.getValue();
        assertThat(payload.keySet()).containsExactlyInAnyOrder(
                "messageId", "message", "senderId", "senderName", "timestamp");
        assertThat(payload.get("message")).isEqualTo("안녕");
        assertThat(payload.get("senderId")).isEqualTo(2L);
        assertThat(payload.get("senderName")).isEqualTo("익명1");
        assertThat(payload.get("timestamp")).isInstanceOf(Long.class);

        String messageId = (String) payload.get("messageId");
        BlindDateMessage recorded = sessions.findMessage(sessionId, messageId).orElseThrow();
        assertThat(recorded.senderId()).isEqualTo(2L);
        assertThat(recorded.content()).isEqualTo("안녕");
        assertThat(recorded.sentAt()).isNotNull();
    }

    @Test
    @DisplayName("긴 메시지는 원문 그대로 방송하고, 기록에는 이모지를 쪼개지 않고 1,000자까지만 남긴다")
    @SuppressWarnings("unchecked")
    void execute_LongMessage_BroadcastsOriginalAndRecordsLimited() {
        String sessionId = sessions.create().getSessionId();
        participants.addParticipant(sessionId, 2L, "socket2");
        String longMessage = "가".repeat(999) + "😀" + "나".repeat(500);

        handler.execute(sessionId, 2L, longMessage);

        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq(BlindDateTopic.message(sessionId)), event.capture());
        Map<String, Object> payload = (Map<String, Object>) event.getValue();
        assertThat(payload.get("message")).isEqualTo(longMessage);

        BlindDateMessage recorded = sessions.findMessage(sessionId, (String) payload.get("messageId")).orElseThrow();
        assertThat(recorded.content()).isEqualTo("가".repeat(999));
    }

    @Test
    @DisplayName("참가자가 아닌 발신자의 메시지는 기존처럼 방송하지 않고 기록도 남기지 않는다")
    void execute_UnknownSender_NeitherRecordsNorBroadcasts() {
        String sessionId = sessions.create().getSessionId();

        handler.execute(sessionId, 9L, "안녕");

        verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
        sessions.recordMessage(sessionId, new BlindDateMessage("probe", 1L, "확인", LocalDateTime.now()));
        assertThat(sessions.findMessagesBefore(sessionId, "probe", 10)).isEmpty();
    }
}
