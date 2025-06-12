package com.dongsoop.dongsoop.chat.controller;

import com.dongsoop.dongsoop.chat.dto.CreateGroupRoomRequest;
import com.dongsoop.dongsoop.chat.dto.CreateRoomRequest;
import com.dongsoop.dongsoop.chat.dto.KickUserRequest;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
        Long targetUserId = request.getTargetUserId();

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

    @GetMapping("/room/{roomId}/messages/since-join")
    public ResponseEntity<List<ChatMessage>> getMessagesSinceJoin(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        List<ChatMessage> messages = chatService.getMessagesSinceJoin(roomId, currentUserId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/room/{roomId}/messages/after/{lastMessageId}")
    public ResponseEntity<List<ChatMessage>> getMessagesAfter(
            @PathVariable("roomId") String roomId,
            @PathVariable("lastMessageId") String lastMessageId) {
        Long currentUserId = getCurrentUserId();
        List<ChatMessage> messages = chatService.getMessagesAfter(roomId, currentUserId, lastMessageId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/room/{roomId}/participants")
    public ResponseEntity<Map<Long, String>> getRoomParticipants(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        chatService.enterChatRoom(roomId, currentUserId);

        ChatRoom room = chatService.getChatRoomById(roomId);
        Map<Long, String> participants = buildParticipantsMap(room);

        return ResponseEntity.ok(participants);
    }

    @PostMapping("/room/group")
    public ResponseEntity<ChatRoom> createGroupRoom(@RequestBody CreateGroupRoomRequest request) {
        Long currentUserId = getCurrentUserId();
        Set<Long> participantIds = request.getParticipants();

        ChatRoom groupRoom = chatService.createGroupChatRoom(currentUserId, participantIds, request.getTitle());
        return ResponseEntity.ok(groupRoom);
    }

    @PostMapping("/room/{roomId}/kick")
    public ResponseEntity<ChatRoom> kickUserFromRoom(
            @PathVariable("roomId") String roomId,
            @RequestBody KickUserRequest kickUserRequest) {
        Long currentUserId = getCurrentUserId();
        Long userToKickId = kickUserRequest.getUserId();

        ChatRoom updatedRoom = chatService.kickUserFromRoom(roomId, currentUserId, userToKickId);
        return ResponseEntity.ok(updatedRoom);
    }

    @PostMapping("/room/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        chatService.leaveChatRoom(roomId, currentUserId);
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId() {
        Member member = memberService.getMemberReferenceByContext();
        return member.getId();
    }

    private Map<Long, String> buildParticipantsMap(ChatRoom room) {
        return room.getParticipants().stream()
                .collect(Collectors.toMap(
                        participantId -> participantId,
                        memberService::getNicknameById
                ));
    }
}