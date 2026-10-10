package com.dongsoop.dongsoop.common.handler.websocket;

import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatAuthorizationInterceptor implements ChannelInterceptor {

    private static final String MESSAGE_DESTINATION_PREFIX = "/app/message/";
    private static final String ENTER_DESTINATION_PREFIX = "/app/enter/";
    private static final String ROOM_SUBSCRIPTION_PREFIX = "/topic/chat/room/";

    private final ChatValidator chatValidator;

    @Override
    @Nullable
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        String roomId = extractChatRoomId(accessor.getCommand(), accessor.getDestination());
        if (roomId != null) {
            chatValidator.validateUserForRoom(roomId, extractUserId(accessor.getUser()));
        }

        return message;
    }

    private String extractChatRoomId(StompCommand command, String destination) {
        if (StompCommand.SEND == command) {
            String messageRoomId = extractRoomId(destination, MESSAGE_DESTINATION_PREFIX);
            return messageRoomId != null ? messageRoomId : extractRoomId(destination, ENTER_DESTINATION_PREFIX);
        }
        if (StompCommand.SUBSCRIBE == command) {
            return extractRoomId(destination, ROOM_SUBSCRIPTION_PREFIX);
        }
        return null;
    }

    private String extractRoomId(String destination, String prefix) {
        if (destination == null || !destination.startsWith(prefix)) {
            return null;
        }

        String roomId = destination.substring(prefix.length());
        if (roomId.isBlank() || roomId.contains("/")) {
            throw new UnauthorizedChatAccessException();
        }
        return roomId;
    }

    private Long extractUserId(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedChatAccessException();
        }

        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException exception) {
            throw new UnauthorizedChatAccessException(exception);
        }
    }
}
