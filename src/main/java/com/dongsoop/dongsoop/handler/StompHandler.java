package com.dongsoop.dongsoop.handler;

import com.dongsoop.dongsoop.exception.domain.websocket.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.jwt.JwtUtil;
import com.dongsoop.dongsoop.jwt.JwtValidator;
import com.dongsoop.dongsoop.member.service.MemberDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {
    private static final String PREFIX = "Bearer";
    private static final Integer TOKEN_START_INDEX = 7;

    private final JwtValidator jwtValidator;
    private final JwtUtil jwtUtil;
    private final MemberDetailsService memberDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
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
            jwtValidator.validate(token);
        } catch (Exception e) {
            throw new UnauthorizedChatAccessException(e);
        }
    }

    private void setAuthentication(StompHeaderAccessor accessor, String token) {
        try {
            String email = jwtUtil.getNameByToken(token);
            UserDetails userDetails = memberDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            accessor.setUser(authentication);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            throw new UnauthorizedChatAccessException(e);
        }
    }
}