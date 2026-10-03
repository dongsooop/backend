package com.dongsoop.dongsoop.common.handler.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.session.WebSocketSessionManager;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.ArrayList;
import java.util.List;

class StompChoiceErrorBrokerTest {
    private static final String TOPIC = "/topic/blinddate/session/session-1/member/1/choice-error";

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L})
    void clientForgeryIsRejectedAndServerCanStillPublish(long sender) {
        try (var broker = new BrokerFixture()) {
            broker.subscribe("owner", 1L, TOPIC);
            assertThatThrownBy(() -> broker.clientSend(sender, TOPIC, "forged-error"))
                    .hasRootCauseInstanceOf(UnauthorizedChatAccessException.class);
            assertThat(broker.delivered).isEmpty();

            broker.serverSend(TOPIC, "server-error");
            assertThat(broker.delivered).hasSize(1);
            assertThat(broker.delivered.get(0).getPayload()).isEqualTo("server-error");
            assertThat(SimpMessageHeaderAccessor.getSessionId(broker.delivered.get(0).getHeaders()))
                    .isEqualTo("owner");
        }
    }

    @Test
    void anotherMemberCannotReceiveOwnersServerError() {
        try (var broker = new BrokerFixture()) {
            broker.subscribe("owner", 1L, TOPIC);
            assertThatThrownBy(() -> broker.subscribe("attacker", 2L, TOPIC))
                    .hasRootCauseInstanceOf(UnauthorizedChatAccessException.class);
            assertThatThrownBy(() -> broker.subscribe("attacker", 2L, "/topic/**"))
                    .hasRootCauseInstanceOf(UnauthorizedChatAccessException.class);
            broker.serverSend(TOPIC, "server-error");
            assertThat(broker.delivered).hasSize(1);
            assertThat(SimpMessageHeaderAccessor.getSessionId(broker.delivered.get(0).getHeaders()))
                    .isEqualTo("owner");
        }
    }

    private static final class BrokerFixture implements AutoCloseable {
        private final ExecutorSubscribableChannel inbound = new ExecutorSubscribableChannel();
        private final ExecutorSubscribableChannel internal = new ExecutorSubscribableChannel();
        private final SimpleBrokerMessageHandler broker;
        private final List<Message<?>> delivered = new ArrayList<>();

        private BrokerFixture() {
            var outbound = new ExecutorSubscribableChannel();
            outbound.subscribe(
                    message -> {
                        if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders())
                                == SimpMessageType.MESSAGE) delivered.add(message);
                    });
            inbound.addInterceptor(
                    new StompHandler(
                            mock(JwtValidator.class),
                            mock(JwtUtil.class),
                            mock(WebSocketSessionManager.class)));
            broker = new SimpleBrokerMessageHandler(inbound, outbound, internal, List.of("/topic"));
            broker.start();
        }

        private void subscribe(String socket, long member, String destination) {
            // CONNECT 인증 자체가 아니라 인증된 연결 이후 채널과 브로커를 검증한다.
            var connect = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT);
            connect.setSessionId(socket);
            broker.handleMessage(
                    MessageBuilder.createMessage(new byte[0], connect.getMessageHeaders()));
            inbound.send(frame(StompCommand.SUBSCRIBE, socket, member, destination, ""));
        }

        private void clientSend(long member, String destination, String payload) {
            inbound.send(
                    frame(StompCommand.SEND, "sender-" + member, member, destination, payload));
        }

        private void serverSend(String destination, String payload) {
            var headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
            headers.setDestination(destination);
            internal.send(MessageBuilder.createMessage(payload, headers.getMessageHeaders()));
        }

        private Message<String> frame(
                StompCommand command,
                String socket,
                long member,
                String destination,
                String payload) {
            var headers = StompHeaderAccessor.create(command);
            headers.setSessionId(socket);
            headers.setSubscriptionId("sub");
            headers.setDestination(destination);
            headers.setUser(new UsernamePasswordAuthenticationToken(member, null, List.of()));
            headers.setLeaveMutable(true);
            return MessageBuilder.createMessage(payload, headers.getMessageHeaders());
        }

        @Override
        public void close() {
            broker.stop();
        }
    }
}
