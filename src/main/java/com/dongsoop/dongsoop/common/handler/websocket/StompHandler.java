package com.dongsoop.dongsoop.common.handler.websocket;

import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.session.WebSocketSessionManager;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.jwt.dto.AuthenticationInformationByToken;
import io.jsonwebtoken.Claims;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {
    private static final String PREFIX = "Bearer";
    private static final Integer TOKEN_START_INDEX = 7;
    private static final AntPathMatcher SUBSCRIPTION_MATCHER = new AntPathMatcher();
    private static final Pattern CHOICE_ERROR_TOPIC = Pattern.compile(
            "^/topic/blinddate/session/[^/]+/member/([^/]+)/choice-error$");

    private final JwtValidator jwtValidator;
    private final JwtUtil jwtUtil;
    private final WebSocketSessionManager sessionManager;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }
        processCommand(accessor, accessor.getCommand());
        return message;
    }

    private void processCommand(StompHeaderAccessor accessor, StompCommand command) {
        if (StompCommand.CONNECT == command) {
            authenticateConnection(accessor);
        }
        if (StompCommand.SEND == command) {
            rejectClientChoiceErrorSend(accessor);
        }
        if (StompCommand.SUBSCRIBE == command) {
            authorizeChoiceErrorSubscription(accessor);
        }
        if (StompCommand.DISCONNECT == command) {
            handleDisconnect(accessor);
        }
    }

    /** 선택 오류는 서버만 발행한다. 서버의 brokerChannel 전송에는 이 검사가 적용되지 않는다. */
    private void rejectClientChoiceErrorSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination != null && CHOICE_ERROR_TOPIC.matcher(destination).matches()) {
            throw new UnauthorizedChatAccessException();
        }
    }

    /** 개인 선택 오류는 인증된 수신자만 구독한다. 선택 시작 전 사전 구독은 허용한다. */
    private void authorizeChoiceErrorSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        rejectPrivateTopicPatterns(destination);
        var match = CHOICE_ERROR_TOPIC.matcher(destination);
        if (!match.matches()) {
            return;
        }
        if (!(accessor.getUser() instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || !match.group(1).equals(authentication.getName())) {
            throw new UnauthorizedChatAccessException();
        }
    }

    /** 광역 패턴 구독으로 개인 토픽의 수신자 검사를 우회하지 못하게 한다. */
    private void rejectPrivateTopicPatterns(String destination) {
        if (SUBSCRIPTION_MATCHER.isPattern(destination)
                && SUBSCRIPTION_MATCHER.matchStart(destination, "/topic/blinddate/session/")) {
            throw new UnauthorizedChatAccessException();
        }
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        validateToken(token);
        setAuthentication(accessor, token);

        Long userId = getUserIdFromToken(token);
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            sessionAttributes.put("memberId", userId);
        }

        String sessionId = accessor.getSessionId();

        if (sessionId != null) {
            sessionManager.addUserSession(userId, sessionId);
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }
    }

    private Long getUserIdFromToken(String token) {
        return jwtUtil.getTokenInformation(token).getId();
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String tokenHeader = accessor.getFirstNativeHeader("Authorization");
        return extractTokenFromHeader(tokenHeader);
    }

    private String extractTokenFromHeader(String tokenHeader) {
        boolean isInvalidToken = !StringUtils.hasText(tokenHeader) ||
                tokenHeader.length() <= TOKEN_START_INDEX ||
                !tokenHeader.startsWith(PREFIX);

        if (isInvalidToken) {
            throw new UnauthorizedChatAccessException();
        }

        return tokenHeader.substring(TOKEN_START_INDEX);
    }

    private void validateToken(String token) {
        try {
            Claims claims = jwtUtil.getClaims(token);
            jwtValidator.validate(claims);
        } catch (Exception e) {
            throw new UnauthorizedChatAccessException(e);
        }
    }

    private void setAuthentication(StompHeaderAccessor accessor, String token) {
        try {
            AuthenticationInformationByToken tokenInfo = jwtUtil.getTokenInformation(token);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    tokenInfo.getId(),
                    null,
                    tokenInfo.getRole()
            );

            accessor.setUser(authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            throw new UnauthorizedChatAccessException(e);
        }
    }
}
