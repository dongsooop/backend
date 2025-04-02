package com.dongsoop.dongsoop.handler;

import com.dongsoop.dongsoop.exception.domain.websocket.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.member.service.MemberDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {
    private static final String PREFIX = "Bearer";
    private static final Integer TOKEN_START_INDEX = 7;

    private final JwtValidator jwtValidator;
    private final JwtUtil jwtUtil;
    private final MemberDetailsService memberDetailsService; // 주입 추가

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.warn("StompAccessor not found in message");
            return message;
        }

        if (StompCommand.CONNECT == accessor.getCommand()) {
            authenticateConnection(accessor);
        }

        return message;
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        validateToken(token);
        setAuthentication(accessor, token);
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String tokenHeader = accessor.getFirstNativeHeader("Authorization");
        return extractTokenFromHeader(tokenHeader);
    }

    private String extractTokenFromHeader(String tokenHeader) {
        if (!StringUtils.hasText(tokenHeader) ||
                tokenHeader.length() <= TOKEN_START_INDEX ||
                !tokenHeader.startsWith(PREFIX)) {
            log.error("WebSocket 연결 거부: 토큰 없음");
            throw new UnauthorizedChatAccessException();
        }
        return tokenHeader.substring(TOKEN_START_INDEX);
    }

    private void validateToken(String token) {
        try {
            jwtValidator.validate(token);
        } catch (Exception e) {
            log.error("WebSocket 연결 거부: 토큰 검증 실패", e);
            throw new UnauthorizedChatAccessException();
        }
    }

    private void setAuthentication(StompHeaderAccessor accessor, String token) {
        try {
            String email = jwtUtil.getNameByToken(token);
            UserDetails userDetails = memberDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            accessor.setUser(authentication);
            log.info("WebSocket 연결 성공: 사용자 = {}", userDetails.getUsername());
        } catch (Exception e) {
            log.error("WebSocket 연결 거부: 사용자 정보 설정 실패", e);
            throw new UnauthorizedChatAccessException();
        }
    }
}