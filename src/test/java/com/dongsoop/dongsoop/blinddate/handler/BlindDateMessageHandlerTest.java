package com.dongsoop.dongsoop.blinddate.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dongsoop.dongsoop.blinddate.config.BlindDateTopic;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@DisplayName("과팅 메시지 핸들러 단위 결과")
class BlindDateMessageHandlerTest {
    @Test
    @DisplayName("B03 발신자·익명 이름·내용·토픽 유지")
    void sendsMemberMessage() {
        var storage = new BlindDateParticipantStorageImpl();
        var member = storage.addParticipant("session", 1L, "socket");
        var messaging = mock(SimpMessagingTemplate.class);
        List<Map<?, ?>> payloads = new ArrayList<>();
        List<String> topics = new ArrayList<>();
        doAnswer(
                        call -> {
                            topics.add(call.getArgument(0));
                            payloads.add(call.getArgument(1));
                            return null;
                        })
                .when(messaging)
                .convertAndSend(anyString(), any(Object.class));
        new BlindDateMessageHandler(storage, messaging).execute("session", 1L, "안녕하세요");
        assertThat(topics).containsExactly(BlindDateTopic.message("session"));
        assertThat(payloads).hasSize(1);
        assertThat(payloads.get(0).get("message")).isEqualTo("안녕하세요");
        assertThat(payloads.get(0).get("senderId")).isEqualTo(1L);
        assertThat(payloads.get(0).get("senderName")).isEqualTo(member.getAnonymousName());
        assertThat(payloads.get(0).get("timestamp")).isInstanceOf(Long.class);
    }

    @Test
    @DisplayName("B04 미등록 회원은 메시지 전송 없음")
    void unknownMemberIsIgnored() {
        var messaging = mock(SimpMessagingTemplate.class);
        new BlindDateMessageHandler(new BlindDateParticipantStorageImpl(), messaging)
                .execute("session", 99L, "text");
        verifyNoInteractions(messaging);
    }
}
