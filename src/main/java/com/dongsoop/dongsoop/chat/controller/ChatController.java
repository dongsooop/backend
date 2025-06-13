package com.dongsoop.dongsoop.chat.controller;

import com.dongsoop.dongsoop.chat.dto.CreateGroupRoomRequest;
import com.dongsoop.dongsoop.chat.dto.CreateRoomRequest;
import com.dongsoop.dongsoop.chat.dto.KickUserRequest;
import com.dongsoop.dongsoop.chat.dto.ReadStatusUpdateRequest;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomInitResponse;
import com.dongsoop.dongsoop.chat.entity.IncrementalSyncResponse;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;
    private final MemberService memberService;

    @GetMapping("/room/{roomId}/initialize")
    public ResponseEntity<ChatRoomInitResponse> initializeChatRoom(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        ChatRoomInitResponse response = chatService.initializeChatRoomForFirstTime(roomId, currentUserId);
        return createSuccessResponse(response);
    }

    @GetMapping("/room/{roomId}/sync")
    public ResponseEntity<IncrementalSyncResponse> syncNewMessages(
            @PathVariable("roomId") String roomId,
            @RequestParam(required = false) String lastMessageId) {
        Long currentUserId = getCurrentUserId();
        IncrementalSyncResponse response = chatService.syncNewMessagesOnly(roomId, currentUserId, lastMessageId);
        return createSuccessResponse(response);
    }

    @PostMapping("/room/{roomId}/read-status")
    public ResponseEntity<Void> updateReadStatus(
            @PathVariable("roomId") String roomId,
            @RequestBody ReadStatusUpdateRequest request) {
        Long currentUserId = getCurrentUserId();
        chatService.updateReadStatus(roomId, currentUserId, request);
        return createSuccessResponse();
    }

    @GetMapping("/room/{roomId}/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        int unreadCount = chatService.getUnreadMessageCount(roomId, currentUserId);
        Map<String, Integer> response = createUnreadCountResponse(unreadCount);
        return createSuccessResponse(response);
    }

    @PostMapping("/room")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody CreateRoomRequest request) {
        Long currentUserId = getCurrentUserId();
        Long targetUserId = request.getTargetUserId();
        ChatRoom createdRoom = chatService.createOneToOneChatRoom(currentUserId, targetUserId);
        return createSuccessResponse(createdRoom);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoom>> getRoomsForUser() {
        Long currentUserId = getCurrentUserId();
        List<ChatRoom> rooms = chatService.getRoomsForUserId(currentUserId);
        return createSuccessResponse(rooms);
    }

    @PostMapping("/room/group")
    public ResponseEntity<ChatRoom> createGroupRoom(@RequestBody CreateGroupRoomRequest request) {
        Long currentUserId = getCurrentUserId();
        ChatRoom groupRoom = chatService.createGroupChatRoom(currentUserId, request.getParticipants(), request.getTitle());
        return createSuccessResponse(groupRoom);
    }

    @GetMapping("/room/{roomId}/enter")
    public ResponseEntity<Void> enterRoom(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        chatService.enterChatRoom(roomId, currentUserId);
        return createSuccessResponse();
    }

    @PostMapping("/room/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        chatService.leaveChatRoom(roomId, currentUserId);
        return createSuccessResponse();
    }

    @PostMapping("/room/{roomId}/kick")
    public ResponseEntity<ChatRoom> kickUser(
            @PathVariable("roomId") String roomId,
            @RequestBody KickUserRequest request) {
        Long currentUserId = getCurrentUserId();
        ChatRoom updatedRoom = chatService.kickUserFromRoom(roomId, currentUserId, request.getUserId());
        return createSuccessResponse(updatedRoom);
    }

    @GetMapping("/room/{roomId}/messages")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        List<ChatMessage> messages = chatService.getChatHistoryForUser(roomId, currentUserId);
        return createSuccessResponse(messages);
    }

    @GetMapping("/room/{roomId}/messages/after/{messageId}")
    public ResponseEntity<List<ChatMessage>> getMessagesAfter(
            @PathVariable("roomId") String roomId,
            @PathVariable("messageId") String messageId) {
        Long currentUserId = getCurrentUserId();
        List<ChatMessage> messages = chatService.getMessagesAfter(roomId, currentUserId, messageId);
        return createSuccessResponse(messages);
    }

    @PostMapping("/room/{roomId}/messages/mark-all-read")
    public ResponseEntity<Void> markAllMessagesAsRead(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        chatService.markAllMessagesAsRead(roomId, currentUserId);
        return createSuccessResponse();
    }

    @PostMapping("/room/{roomId}/messages/sync-offline")
    public ResponseEntity<Map<String, Object>> syncOfflineMessages(
            @PathVariable("roomId") String roomId,
            @RequestBody List<ChatMessage> offlineMessages) {
        Long currentUserId = getCurrentUserId();
        List<ChatMessage> processedMessages = chatService.syncOfflineMessages(roomId, currentUserId, offlineMessages);

        Map<String, Object> response = createOfflineMessageSyncResponse(processedMessages);
        return createSuccessResponse(response);
    }

    private Long getCurrentUserId() {
        Member member = memberService.getMemberReferenceByContext();
        return member.getId();
    }

    private <T> ResponseEntity<T> createSuccessResponse(T body) {
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Void> createSuccessResponse() {
        return ResponseEntity.ok().build();
    }

    private Map<String, Integer> createUnreadCountResponse(int unreadCount) {
        return Map.of("unreadCount", unreadCount);
    }

    private Map<String, Object> createOfflineMessageSyncResponse(List<ChatMessage> processedMessages) {
        return Map.of(
                "processedCount", processedMessages.size(),
                "processedMessages", processedMessages
        );
    }
}