package com.dongsoop.dongsoop.handler;

import com.dongsoop.dongsoop.exception.domain.websocket.UserKickedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Component
public class CustomStompErrorHandler extends StompSubProtocolErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(CustomStompErrorHandler.class);

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        if (ex instanceof UserKickedException) {
            logger.warn("사용자가 강퇴되었습니다: {}", ex.getMessage());
            return createStompErrorMessage(clientMessage, ex.getMessage());
        }
        return super.handleClientMessageProcessingError(clientMessage, ex);
    }

    // STOMP 오류 메시지 생성 메서드 추가
    private Message<byte[]> createStompErrorMessage(Message<byte[]> clientMessage, String errorMessage) {
        StompHeaderAccessor clientHeaderAccessor = StompHeaderAccessor.wrap(clientMessage);
        String sessionId = clientHeaderAccessor.getSessionId();
        
        StompHeaderAccessor errorHeaderAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
        errorHeaderAccessor.setSessionId(sessionId);
        errorHeaderAccessor.setMessage(errorMessage);

        if (clientHeaderAccessor.getUser() != null) {
            errorHeaderAccessor.setUser(clientHeaderAccessor.getUser());
        }

        return MessageBuilder.createMessage(
                errorMessage.getBytes(StandardCharsets.UTF_8),
                errorHeaderAccessor.getMessageHeaders()
        );
    }
}