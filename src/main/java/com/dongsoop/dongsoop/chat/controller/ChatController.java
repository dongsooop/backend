package com.dongsoop.dongsoop.chat.controller;

import com.dongsoop.dongsoop.chat.dto.CreateGroupRoomRequest;
import com.dongsoop.dongsoop.chat.dto.CreateRoomRequest;
import com.dongsoop.dongsoop.chat.dto.MessageSyncRequest;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;

    @PostMapping("/room")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody CreateRoomRequest request) {
        String currentUserId = getCurrentUserId();
        return ResponseEntity.ok(chatService.createOneToOneChatRoom(currentUserId, request.getTargetUserId()));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getRoomsForUser() {
        String currentUserId = getCurrentUserId();
        return ResponseEntity.ok(chatService.getRoomsForUserId(currentUserId));
    }

    @GetMapping("/room/{roomId}/enter")
    public ResponseEntity<Void> enterRoom(@PathVariable("roomId") String roomId) {
        chatService.enterChatRoom(roomId, getCurrentUserId());
        return ResponseEntity.ok().build();
    }

    @MessageMapping("/message/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessage sendMessage(@Payload ChatMessage message, @DestinationVariable("roomId") String roomId, Principal principal) {
        message.setSenderId(principal.getName());
        message.setRoomId(roomId);
        return chatService.processMessage(message);
    }

    @MessageMapping("/enter/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessage enterChatRoom(@DestinationVariable("roomId") String roomId, Principal principal) {
        return chatService.createEnterMessage(roomId, principal.getName());
    }

    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable("roomId") String roomId) {
        return ResponseEntity.ok(chatService.getChatHistory(roomId, getCurrentUserId()));
    }

    @PostMapping("/room/{roomId}/sync")
    public ResponseEntity<List<ChatMessage>> syncMessages(
            @PathVariable("roomId") String roomId,
            @RequestBody MessageSyncRequest request) {
        return ResponseEntity.ok(chatService.syncMessages(roomId, getCurrentUserId(), request.getMessages()));
    }

    @PostMapping("/room/{roomId}/recreate")
    public ResponseEntity<ChatRoom> recreateRoom(
            @PathVariable("roomId") String roomId,
            @RequestBody MessageSyncRequest request) {
        chatService.recreateRoomIfNeeded(roomId, getCurrentUserId(), request.getMessages());
        return ResponseEntity.ok(chatService.getChatRoomById(roomId));
    }

    @PostMapping("/room/group")
    public ResponseEntity<ChatRoom> createGroupRoom(@RequestBody CreateGroupRoomRequest request) {
        String currentUserId = getCurrentUserId();
        return ResponseEntity.ok(chatService.createGroupChatRoom(currentUserId, request.getParticipants()));
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}