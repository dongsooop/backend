package com.dongsoop.dongsoop.chat.controller;

import com.dongsoop.dongsoop.chat.dto.CreateGroupRoomRequest;
import com.dongsoop.dongsoop.chat.dto.CreateRoomRequest;
import com.dongsoop.dongsoop.chat.dto.KickUserRequest;
import com.dongsoop.dongsoop.chat.dto.MessageSyncRequest;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.member.dto.LoginAuthenticate;
import com.dongsoop.dongsoop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;
    private final MemberService memberService;

    @PostMapping("/room")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody CreateRoomRequest request) {
        Long currentUserId = getCurrentUserId();

        LoginAuthenticate targetUserAuth = memberService.getLoginAuthenticateByNickname(request.getTargetUserId());
        Long targetUserId = targetUserAuth.getId();

        return ResponseEntity.ok(chatService.createOneToOneChatRoom(currentUserId, targetUserId));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getRoomsForUser() {
        Long currentUserId = getCurrentUserId();
        return ResponseEntity.ok(chatService.getRoomsForUserId(currentUserId));
    }

    @GetMapping("/room/{roomId}/enter")
    public ResponseEntity<Void> enterRoom(@PathVariable("roomId") String roomId) {
        chatService.enterChatRoom(roomId, getCurrentUserId());
        return ResponseEntity.ok().build();
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
        Long currentUserId = getCurrentUserId();

        Set<Long> participantIds = request.getParticipants().stream()
                .map(nickname -> memberService.getLoginAuthenticateByNickname(nickname).getId())
                .collect(Collectors.toSet());

        return ResponseEntity.ok(chatService.createGroupChatRoom(currentUserId, participantIds));
    }

    @PostMapping("/room/{roomId}/kick")
    public ResponseEntity<ChatRoom> kickUserFromRoom(
            @PathVariable("roomId") String roomId,
            @RequestBody KickUserRequest kickUserRequest) {
        Long currentUserId = getCurrentUserId();

        Long userToKickId = memberService.getLoginAuthenticateByNickname(kickUserRequest.getUserId()).getId();

        return ResponseEntity.ok(chatService.kickUserFromRoom(roomId, currentUserId, userToKickId));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getName());
    }
}