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

import java.util.Collections;
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

        ChatRoom createdRoom = chatService.createOneToOneChatRoom(currentUserId, targetUserId);
        return ResponseEntity.ok(createdRoom);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getRoomsForUser() {
        Long currentUserId = getCurrentUserId();
        List<ChatRoom> rooms = chatService.getRoomsForUserId(currentUserId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/room/{roomId}/enter")
    public ResponseEntity<Void> enterRoom(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        chatService.enterChatRoom(roomId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        List<ChatMessage> messages = chatService.getChatHistory(roomId, currentUserId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/room/{roomId}/sync")
    public ResponseEntity<List<ChatMessage>> syncMessages(
            @PathVariable("roomId") String roomId,
            @RequestBody MessageSyncRequest request) {
        Long currentUserId = getCurrentUserId();
        List<ChatMessage> syncedMessages = chatService.syncMessages(roomId, currentUserId, request.getMessages());
        return ResponseEntity.ok(syncedMessages);
    }

    @PostMapping("/room/{roomId}/recreate")
    public ResponseEntity<ChatRoom> recreateRoom(
            @PathVariable("roomId") String roomId,
            @RequestBody MessageSyncRequest request) {
        Long currentUserId = getCurrentUserId();
        chatService.recreateRoomIfNeeded(roomId, currentUserId, request.getMessages());

        ChatRoom recreatedRoom = chatService.getChatRoomById(roomId);
        return ResponseEntity.ok(recreatedRoom);
    }

    @PostMapping("/room/group")
    public ResponseEntity<ChatRoom> createGroupRoom(@RequestBody CreateGroupRoomRequest request) {
        Long currentUserId = getCurrentUserId();
        Set<Long> participantIds = convertNicknamesToIds(request.getParticipants());

        ChatRoom groupRoom = chatService.createGroupChatRoom(currentUserId, participantIds, request.getTitle());
        return ResponseEntity.ok(groupRoom);
    }

    @PostMapping("/room/{roomId}/kick")
    public ResponseEntity<ChatRoom> kickUserFromRoom(
            @PathVariable("roomId") String roomId,
            @RequestBody KickUserRequest kickUserRequest) {
        Long currentUserId = getCurrentUserId();
        String userToKickNickname = kickUserRequest.getUserId();
        Long userToKickId = memberService.getLoginAuthenticateByNickname(userToKickNickname).getId();

        ChatRoom updatedRoom = chatService.kickUserFromRoom(roomId, currentUserId, userToKickId);
        return ResponseEntity.ok(updatedRoom);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getName());
    }

    private Set<Long> convertNicknamesToIds(Set<String> nicknames) {
        Set<String> safeNicknames = getSafeNicknames(nicknames);
        return safeNicknames.stream()
                .map(this::convertNicknameToId)
                .collect(Collectors.toSet());
    }

    private Set<String> getSafeNicknames(Set<String> nicknames) {
        if (nicknames != null) {
            return nicknames;
        }
        return Collections.emptySet();
    }

    private Long convertNicknameToId(String nickname) {
        LoginAuthenticate userAuth = memberService.getLoginAuthenticateByNickname(nickname);
        return userAuth.getId();
    }
}