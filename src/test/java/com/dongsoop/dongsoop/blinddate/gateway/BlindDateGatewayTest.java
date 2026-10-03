package com.dongsoop.dongsoop.blinddate.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dongsoop.dongsoop.blinddate.dto.BlindDateChoiceDto;
import com.dongsoop.dongsoop.blinddate.dto.BlindDateMessageDto;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateDisconnectHandler;
import com.dongsoop.dongsoop.blinddate.support.BlindDateScenarioFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.HashMap;
import java.util.Map;

@Timeout(10)
@DisplayName("과팅 게이트웨이 소켓 경계")
class BlindDateGatewayTest {
    private BlindDateGateway gateway(BlindDateScenarioFixture flow) {
        return new BlindDateGateway(flow.connect, flow.disconnect, flow.message, flow.choice);
    }

    private SimpMessageHeaderAccessor attributes(Map<String, Object> attributes) {
        var accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        accessor.setSessionId("socket");
        accessor.setSessionAttributes(attributes);
        return accessor;
    }

    private SessionSubscribeEvent subscription(String destination, Map<String, Object> attributes) {
        var headers = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        headers.setSessionId("socket");
        headers.setDestination(destination);
        headers.setSessionAttributes(attributes);
        return new SessionSubscribeEvent(
                this, MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders()));
    }

    @Test
    @DisplayName("B05 과팅 입장 토픽 구독만 참가자를 등록")
    void onlyJoinSubscriptionCreatesParticipant() {
        try (var flow = new BlindDateScenarioFixture(3)) {
            Map<String, Object> attributes = new HashMap<>(Map.of("memberId", 1L));
            gateway(flow).handleConnect(subscription("/unrelated", attributes));
            flow.queue.awaitIdle();
            assertThat(flow.participants.getByMemberId(1L)).isNull();
            gateway(flow).handleConnect(subscription("/user/queue/blinddate/join", attributes));
            flow.queue.awaitIdle();
            assertThat(flow.participants.getBySocketId("socket").getMemberId()).isEqualTo(1L);
            assertThat(attributes.get("sessionId")).isNotNull();
        }
    }

    @ParameterizedTest(name = "B06 누락 속성: {0}")
    @ValueSource(strings = {"attributes", "member", "session"})
    void missingIdentityDoesNotSendMessageOrChoice(String missing) {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            flow.queue.openChoices(session, 10_000);
            Map<String, Object> data = new HashMap<>(Map.of("memberId", 1L, "sessionId", session));
            if (missing.equals("attributes")) data = null;
            else if (missing.equals("member")) data.remove("memberId");
            else data.remove("sessionId");
            var accessor = attributes(data);
            flow.events.clear();
            gateway(flow).handleMessage(new BlindDateMessageDto("text"), accessor);
            gateway(flow).handleChoice(new BlindDateChoiceDto(2L), accessor);
            flow.choice.execute(session, 2L, 1L);
            flow.queue.awaitIdle();
            assertThat(flow.events).isEmpty();
            assertThat(flow.createdPairs).isEmpty();
        }
    }

    @Test
    @DisplayName("B07 서버 소켓 속성의 회원·세션으로 메시지와 선택 처리")
    void authenticatedAttributesDetermineSenderAndChoice() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            flow.queue.openChoices(session, 10_000);
            var accessor = attributes(new HashMap<>(Map.of("memberId", 1L, "sessionId", session)));
            flow.events.clear();
            gateway(flow).handleMessage(new BlindDateMessageDto("text"), accessor);
            gateway(flow).handleChoice(new BlindDateChoiceDto(2L), accessor);
            flow.choice.execute(session, 2L, 1L);
            flow.queue.awaitIdle();
            var payload = (Map<?, ?>) flow.events.get(0).payload();
            assertThat(payload.get("senderId")).isEqualTo(1L);
            assertThat(payload.get("message")).isEqualTo("text");
            assertThat(flow.recipients("/chatroom")).containsExactlyInAnyOrder(1L, 2L);
        }
    }

    @Test
    @DisplayName("B08 연결 해제 속성 누락과 예외는 호출자에게 전파하지 않음")
    void malformedDisconnectAndHandlerFailureAreContained() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            var handler = mock(BlindDateDisconnectHandler.class);
            var gateway = new BlindDateGateway(flow.connect, handler, flow.message, flow.choice);
            var missing = attributes(null);
            gateway.handleDisconnect(
                    new SessionDisconnectEvent(
                            this,
                            MessageBuilder.createMessage(new byte[0], missing.getMessageHeaders()),
                            "socket",
                            CloseStatus.NORMAL));
            verifyNoInteractions(handler);
            doThrow(new IllegalStateException("disconnect failed"))
                    .when(handler)
                    .execute(anyString(), anyLong(), anyString());
            var headers = attributes(new HashMap<>(Map.of("memberId", 1L, "sessionId", "session")));
            assertThatCode(
                            () ->
                                    gateway.handleDisconnect(
                                            new SessionDisconnectEvent(
                                                    this,
                                                    MessageBuilder.createMessage(
                                                            new byte[0],
                                                            headers.getMessageHeaders()),
                                                    "socket",
                                                    CloseStatus.NORMAL)))
                    .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("회원이나 속성이 없는 입장 구독은 무시")
    void missingJoinIdentityIsIgnored() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            gateway(flow).handleConnect(subscription("/user/queue/blinddate/join", null));
            gateway(flow)
                    .handleConnect(subscription("/user/queue/blinddate/join", new HashMap<>()));
            flow.queue.awaitIdle();
            assertThat(flow.operation.getPointer()).isNull();
        }
    }
}
