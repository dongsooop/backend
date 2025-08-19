package com.dongsoop.dongsoop.chat.controller;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.memberblock.constant.BlockStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;


@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/message/{roomId}")
    public void sendMessage(
            @Payload ChatMessage message,
            @DestinationVariable("roomId") String roomId,
            Principal principal) {

        Long userId = extractUserIdFromPrincipal(principal);

        BlockStatus status = chatService.getBlockStatus(roomId, userId);
        if (status == BlockStatus.I_BLOCKED) {
            return;
        }

        ChatMessage processedMessage = chatService.processWebSocketMessage(message, userId, roomId);

        if (processedMessage != null) {
            messagingTemplate.convertAndSend("/topic/chat/room/" + roomId, processedMessage);
        }
    }

    @MessageMapping("/enter/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessage enterChatRoom(@DestinationVariable("roomId") String roomId, Principal principal) {
        Long userId = extractUserIdFromPrincipal(principal);

        ChatMessage enterMsg = chatService.processWebSocketEnter(roomId, userId);
        BlockStatus blockStatus = chatService.getBlockStatus(roomId, userId);
        chatService.sendBlockStatusToUser(roomId, userId, blockStatus);

        return enterMsg;
    }

    private Long extractUserIdFromPrincipal(Principal principal) {
        return Long.parseLong(principal.getName());
    }
}