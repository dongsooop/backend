package com.dongsoop.dongsoop.chat.controller;

import com.dongsoop.dongsoop.chat.dto.CreateRoomRequest;
import com.dongsoop.dongsoop.chat.dto.MessageSyncRequest;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/chat/room")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody CreateRoomRequest request) {
        String currentUserId = getCurrentUserId();
        log.info("1:1 채팅방 생성 요청: userId={}, targetUserId={}", currentUserId, request.getTargetUserId());
        return ResponseEntity.ok(chatService.createOneToOneChatRoom(currentUserId, request.getTargetUserId()));
    }

    @GetMapping("/chat/room/{roomId}/enter")
    public ResponseEntity<Void> enterRoom(@PathVariable("roomId") String roomId) {
        String currentUserId = getCurrentUserId();
        log.info("사용자 {} 채팅방 {} 입장", currentUserId, roomId);
        chatService.enterChatRoom(roomId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/chat/message/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessage sendMessage(@Payload ChatMessage message, @DestinationVariable("roomId") String roomId, Principal principal) {
        String userId = principal.getName();
        message.setSenderId(userId);
        message.setRoomId(roomId);
        log.info("메시지 전송: 사용자={}, 메시지={}", userId, message);
        return chatService.processMessage(message);
    }

    @MessageMapping("/chat/enter/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessage enterChatRoom(@DestinationVariable("roomId") String roomId, Principal principal) {
        String userId = principal.getName();
        log.info("WebSocket을 통한 채팅방 입장: roomId={}, userId={}", roomId, userId);
        return chatService.createEnterMessage(roomId, userId);
    }

    @GetMapping("/chat/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable("roomId") String roomId) {
        String currentUserId = getCurrentUserId();
        return ResponseEntity.ok(chatService.getChatHistory(roomId, currentUserId));
    }

    @PostMapping("/chat/room/{roomId}/sync")
    public ResponseEntity<List<ChatMessage>> syncMessages(
            @PathVariable("roomId") String roomId,
            @RequestBody MessageSyncRequest request) {
        String currentUserId = getCurrentUserId();
        log.info("메시지 동기화 요청: roomId={}, userId={}, 메시지 수={}",
                roomId, currentUserId, request.getMessages().size());
        return ResponseEntity.ok(chatService.syncMessages(roomId, currentUserId, request.getMessages()));
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}