package com.dongsoop.dongsoop.chat.controller;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.member.service.MemberService;
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
    private final MemberService memberService;  // 추가

    @MessageMapping("/message/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessage sendMessage(
            @Payload ChatMessage message,
            @DestinationVariable("roomId") String roomId,
            Principal principal) {

        Long userId = extractUserIdFromPrincipal(principal);
        String userNickname = getUserNicknameById(userId);

        message.setSenderId(userId);
        message.setSenderNickName(userNickname);
        message.setRoomId(roomId);

        return chatService.processMessage(message);
    }

    @MessageMapping("/enter/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessage enterChatRoom(
            @DestinationVariable("roomId") String roomId,
            Principal principal) {

        Long userId = extractUserIdFromPrincipal(principal);
        return chatService.createEnterMessage(roomId, userId);
    }

    private Long extractUserIdFromPrincipal(Principal principal) {
        return Long.parseLong(principal.getName());
    }

    private String getUserNicknameById(Long userId) {
        return memberService.getNicknameById(userId);
    }
}