package com.dongsoop.dongsoop.blinddate.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.session.WebSocketSessionManager;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.common.config.WebSocketConfig;
import com.dongsoop.dongsoop.common.handler.websocket.ChatAuthorizationInterceptor;
import com.dongsoop.dongsoop.common.handler.websocket.CustomStompErrorHandler;
import com.dongsoop.dongsoop.common.handler.websocket.StompHandler;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class StompChoiceErrorBrokerTest {
    private static final String TOPIC = "/topic/blinddate/session/session-1/member/1/choice-error";
    private static final String PUBLIC_TOPIC = "/topic/blinddate/session/session-1/start";

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

    @ParameterizedTest(name = "raw {0}, sender={1}")
    @CsvSource({"SEND,1", "SEND,2", "MESSAGE,1", "MESSAGE,2"})
    void rawClientFrameCannotForgeServerError(StompCommand command, long member) {
        try (var fixture = new BrokerFixture()) {
            fixture.subscribe("owner", 1L, TOPIC);
            fixture.rawClientSend(command, member, TOPIC, "forged-error");
            assertThat(fixture.delivered).isEmpty();
            fixture.serverSend(TOPIC, "server-error");
            assertThat(fixture.delivered)
                    .singleElement()
                    .satisfies(
                            message -> {
                                assertThat(message.getPayload()).isEqualTo("server-error");
                                assertThat(
                                                SimpMessageHeaderAccessor.getSessionId(
                                                        message.getHeaders()))
                                        .isEqualTo("owner");
                            });
        }
    }

    @Test
    void normalClientSendStillReachesBroker() {
        try (var broker = new BrokerFixture()) {
            broker.subscribe("owner", 1L, PUBLIC_TOPIC);

            broker.clientSend(1L, PUBLIC_TOPIC, "client-message");

            assertThat(broker.delivered).hasSize(1);
            assertThat(broker.delivered.get(0).getPayload()).isEqualTo("client-message");
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
            configuredInterceptors().forEach(inbound::addInterceptor);
            broker = new SimpleBrokerMessageHandler(inbound, outbound, internal, List.of("/topic"));
            broker.start();
        }

        private List<ChannelInterceptor> configuredInterceptors() {
            var stompHandler =
                    new StompHandler(
                            mock(JwtValidator.class),
                            mock(JwtUtil.class),
                            mock(WebSocketSessionManager.class),
                            mock(DeviceBlacklistService.class));
            var registration = new InspectableChannelRegistration();
            new WebSocketConfig(
                            stompHandler,
                            new BlindDateStompAuthorizationInterceptor(),
                            new ChatAuthorizationInterceptor(mock(ChatValidator.class)),
                            new CustomStompErrorHandler())
                    .configureClientInboundChannel(registration);
            return registration.configuredInterceptors();
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

        private void rawClientSend(
                StompCommand command, long member, String destination, String payload) {
            var socket = mock(WebSocketSession.class);
            when(socket.getId()).thenReturn("sender-" + member);
            when(socket.isOpen()).thenReturn(true);
            when(socket.getAttributes()).thenReturn(new HashMap<>());
            when(socket.getPrincipal())
                    .thenReturn(new UsernamePasswordAuthenticationToken(member, null, List.of()));
            var protocol = new StompSubProtocolHandler();
            protocol.afterSessionStarted(socket, inbound);
            protocol.handleMessageFromClient(
                    socket,
                    new TextMessage(
                            command.name()
                                    + "\ndestination:"
                                    + destination
                                    + "\n\n"
                                    + payload
                                    + "\u0000"),
                    inbound);
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

    private static final class InspectableChannelRegistration extends ChannelRegistration {
        private List<ChannelInterceptor> configuredInterceptors() {
            return getInterceptors();
        }
    }
}
