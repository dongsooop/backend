package com.dongsoop.dongsoop.blinddate.gateway;

import com.dongsoop.dongsoop.blinddate.dto.BlindDateChoiceDto;
import com.dongsoop.dongsoop.blinddate.dto.BlindDateMessageDto;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateConnectHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateDisconnectHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateMessageHandler;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * 과팅 WebSocket Gateway
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class BlindDateGateway {

    private final BlindDateConnectHandler connectHandler;
    private final BlindDateDisconnectHandler disconnectHandler;
    private final BlindDateMessageHandler messageHandler;
    private final BlindDateChoiceHandler choiceHandler;

    /**
     * WebSocket 구독 이벤트 - BlindDate 자동 입장
     */
    @EventListener
    public void handleConnect(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();

        // "/user/queue/blinddate/join" 구독 시에만 처리
        if (destination == null || !destination.equals("/user/queue/blinddate/join")) {
            return;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return;
        }

        String socketId = accessor.getSessionId();
        Long memberId = (Long) sessionAttributes.get("memberId");
        if (memberId == null) {
            log.info("[BlindDate] Member id not initialized");
            return;
        }

        connectHandler.execute(socketId, memberId, sessionAttributes);
    }

    /**
     * WebSocket 연결 해제 시 - 소켓만 제거, 모든 소켓 제거되면 퇴장 처리
     */
    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String socketId = accessor.getSessionId();

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.warn("[BlindDate] sessionAttributes is null for disconnect: socketId={}", socketId);
            return;
        }

        Long memberId = (Long) sessionAttributes.get("memberId");
        String sessionId = (String) sessionAttributes.get("sessionId");

        try {
            disconnectHandler.execute(socketId, memberId, sessionId);
        } catch (Exception e) {
            log.error("Failed to handle disconnect: socketId={}, memberId={}", socketId, memberId, e);
        }
    }

    /**
     * 메시지 전송
     */
    @MessageMapping("/blinddate/message")
    public void handleMessage(@Payload BlindDateMessageDto message, SimpMessageHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.error("[BlindDate] Session attributes not found");
            return;
        }

        Long memberId = (Long) sessionAttributes.get("memberId");
        String sessionId = (String) sessionAttributes.get("sessionId");

        if (memberId == null || sessionId == null) {
            log.error("[BlindDate] Member or session data not found");
            return;
        }

        // 메시지 브로드캐스트
        messageHandler.execute(sessionId, memberId, message.getMessage());
    }

    /**
     * 사랑의 작대기 - 선택
     */
    @MessageMapping("/blinddate/choice")
    public void handleChoice(@Payload BlindDateChoiceDto choice, SimpMessageHeaderAccessor accessor) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            log.error("Session attributes not found");
            return;
        }

        Long memberId = (Long) sessionAttributes.get("memberId");
        String sessionId = (String) sessionAttributes.get("sessionId");

        if (memberId == null || sessionId == null) {
            log.error("Member or session data not found");
            return;
        }

        // 선택 처리
        choiceHandler.execute(sessionId, memberId, choice.getTargetId());
    }
}
