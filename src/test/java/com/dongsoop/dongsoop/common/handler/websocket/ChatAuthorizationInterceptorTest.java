package com.dongsoop.dongsoop.common.handler.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ChatAuthorizationInterceptorTest {

    @Mock
    private ChatValidator chatValidator;

    @Mock
    private MessageChannel channel;

    @InjectMocks
    private ChatAuthorizationInterceptor interceptor;

    @Test
    void authorizesChatMessageSend() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/message/room-1", true);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verify(chatValidator).validateUserForRoom("room-1", 1L);
    }

    @Test
    void authorizesChatRoomEnterSend() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/enter/room-1", true);

        interceptor.preSend(message, channel);

        verify(chatValidator).validateUserForRoom("room-1", 1L);
    }

    @Test
    void authorizesChatRoomSubscription() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/chat/room/room-1", true);

        interceptor.preSend(message, channel);

        verify(chatValidator).validateUserForRoom("room-1", 1L);
    }

    @Test
    void rejectsProtectedDestinationWithoutAuthenticatedUser() {
        Message<byte[]> message = stompMessage(StompCommand.SUBSCRIBE, "/topic/chat/room/room-1", false);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(UnauthorizedChatAccessException.class);
    }

    @Test
    void ignoresBlindDateDestination() {
        Message<byte[]> message = stompMessage(StompCommand.SEND, "/app/blinddate/message", true);

        Message<?> result = interceptor.preSend(message, channel);

        assertThat(result).isSameAs(message);
        verifyNoInteractions(chatValidator);
    }

    private Message<byte[]> stompMessage(StompCommand command, String destination, boolean authenticated) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        if (authenticated) {
            accessor.setUser(new UsernamePasswordAuthenticationToken(1L, null, List.of()));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
