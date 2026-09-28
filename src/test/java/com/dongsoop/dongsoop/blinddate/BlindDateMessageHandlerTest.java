package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateMessageHandler;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class BlindDateMessageHandlerTest {

    @InjectMocks
    private BlindDateMessageHandler handler;

    @Mock
    private BlindDateParticipantStorage participantStorage;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private BlindDateSessionStorage sessionStorage;

    @Test
    @DisplayName("메시지에 ID를 붙여 보관하고 같은 ID로 브로드캐스트한다")
    @SuppressWarnings("unchecked")
    void execute_RecordsAndBroadcastsWithSameId() {
        when(participantStorage.getAnonymousName(2L)).thenReturn("익명1");

        handler.execute("s1", 2L, "안녕");

        ArgumentCaptor<BlindDateMessage> recorded = ArgumentCaptor.forClass(BlindDateMessage.class);
        verify(sessionStorage).recordMessage(eq("s1"), recorded.capture());
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq(BlindDateTopic.message("s1")), event.capture());

        Map<String, Object> payload = (Map<String, Object>) event.getValue();
        assertThat(payload.get("messageId")).isEqualTo(recorded.getValue().messageId());
        assertThat(recorded.getValue().senderId()).isEqualTo(2L);
        assertThat(recorded.getValue().content()).isEqualTo("안녕");
    }
}
