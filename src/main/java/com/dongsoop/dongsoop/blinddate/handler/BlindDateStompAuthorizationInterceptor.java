package com.dongsoop.dongsoop.blinddate.handler;

import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class BlindDateStompAuthorizationInterceptor implements ChannelInterceptor {
    private static final AntPathMatcher SUBSCRIPTION_MATCHER = new AntPathMatcher();
    private static final Pattern CHOICE_ERROR_TOPIC = Pattern.compile(
            "^/topic/blinddate/session/[^/]+/member/([^/]+)/choice-error$");

    @Override
    @Nullable
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.SEND == command) {
            rejectClientChoiceErrorSend(accessor);
        }
        if (StompCommand.SUBSCRIBE == command) {
            authorizeChoiceErrorSubscription(accessor);
        }
        return message;
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
}
