package com.dongsoop.dongsoop.chat.controller;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chatService;

    @MessageMapping("/message/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessage sendMessage(
            @Payload ChatMessage message,
            @DestinationVariable("roomId") String roomId,
            Principal principal) {
        message.setSenderId(Long.parseLong(principal.getName()));
        message.setRoomId(roomId);
        return chatService.processMessage(message);
    }

    @MessageMapping("/enter/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessage enterChatRoom(
            @DestinationVariable("roomId") String roomId,
            Principal principal) {
        return chatService.createEnterMessage(roomId, Long.parseLong(principal.getName()));
    }
}