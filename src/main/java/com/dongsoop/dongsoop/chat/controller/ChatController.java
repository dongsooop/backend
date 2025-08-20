package com.dongsoop.dongsoop.chat.controller;

import com.dongsoop.dongsoop.chat.dto.ChatRoomListResponse;
import com.dongsoop.dongsoop.chat.dto.CreateContactRoomRequest;
import com.dongsoop.dongsoop.chat.dto.CreateGroupRoomRequest;
import com.dongsoop.dongsoop.chat.dto.CreateRoomRequest;
import com.dongsoop.dongsoop.chat.dto.KickUserRequest;
import com.dongsoop.dongsoop.chat.dto.ReadStatusUpdateRequest;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomInitResponse;
import com.dongsoop.dongsoop.chat.service.ChatMessageService;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final MemberService memberService;

    @GetMapping("/room/{roomId}/initialize")
    public ResponseEntity<ChatRoomInitResponse> initializeChatRoom(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        ChatRoomInitResponse response = chatService.initializeChatRoomForFirstTime(roomId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/room/{roomId}/read-status")
    public ResponseEntity<Void> updateReadStatus(
            @PathVariable("roomId") String roomId,
            @RequestBody ReadStatusUpdateRequest request) {
        Long currentUserId = getCurrentUserId();
        chatService.updateReadStatus(roomId, currentUserId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/room/{roomId}/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        int unreadCount = chatService.getUnreadMessageCount(roomId, currentUserId);
        Map<String, Integer> response = createUnreadCountResponse(unreadCount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/room")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody CreateRoomRequest request) {
        Long currentUserId = getCurrentUserId();
        Long targetUserId = request.getTargetUserId();
        ChatRoom createdRoom = chatRoomService.createOneToOneChatRoom(currentUserId, targetUserId, request.getTitle());
        return ResponseEntity.ok(createdRoom);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomListResponse>> getRoomsForUser() {
        Long currentUserId = getCurrentUserId();
        List<ChatRoom> rooms = chatRoomService.getRoomsForUserId(currentUserId);
        List<ChatRoomListResponse> roomResponses = rooms.stream()
                .map(room -> buildRoomListResponse(room, currentUserId))
                .toList();
        return ResponseEntity.ok(roomResponses);
    }

    @PostMapping("/room/group")
    public ResponseEntity<ChatRoom> createGroupRoom(@RequestBody CreateGroupRoomRequest request) {
        Long currentUserId = getCurrentUserId();
        ChatRoom groupRoom = chatRoomService.createGroupChatRoom(currentUserId, request.getParticipants(),
                request.getTitle());
        return ResponseEntity.ok(groupRoom);
    }

    @PostMapping("/room/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        chatService.leaveChatRoom(roomId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/room/{roomId}/kick")
    public ResponseEntity<ChatRoom> kickUser(
            @PathVariable("roomId") String roomId,
            @RequestBody KickUserRequest request) {
        Long currentUserId = getCurrentUserId();
        ChatRoom updatedRoom = chatService.kickUserFromRoom(roomId, currentUserId, request.getUserId());
        return ResponseEntity.ok(updatedRoom);
    }

    @GetMapping("/room/{roomId}/messages/after/{messageId}")
    public ResponseEntity<List<ChatMessage>> getMessagesAfter(
            @PathVariable("roomId") String roomId,
            @PathVariable("messageId") String messageId) {
        Long currentUserId = getCurrentUserId();
        List<ChatMessage> messages = chatService.getMessagesAfter(roomId, currentUserId, messageId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/room/{roomId}/messages/mark-all-read")
    public ResponseEntity<Void> markAllMessagesAsRead(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        chatService.markAllMessagesAsRead(roomId, currentUserId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/room/{roomId}/messages/sync-offline")
    public ResponseEntity<Map<String, Object>> syncOfflineMessages(
            @PathVariable("roomId") String roomId,
            @RequestBody List<ChatMessage> offlineMessages) {
        Long currentUserId = getCurrentUserId();
        List<ChatMessage> processedMessages = chatService.syncOfflineMessages(roomId, currentUserId, offlineMessages);

        Map<String, Object> response = createOfflineMessageSyncResponse(processedMessages);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/{roomId}/participants")
    public ResponseEntity<Map<Long, String>> getRoomParticipants(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        chatService.validateUserAccess(roomId, currentUserId);

        ChatRoom room = chatRoomService.getChatRoomById(roomId);

        Map<Long, String> participants = room.getParticipants().stream()
                .collect(Collectors.toMap(
                        participantId -> participantId,
                        memberService::getNicknameById
                ));

        return ResponseEntity.ok(participants);
    }

    @PostMapping("/room/contact")
    public ResponseEntity<ChatRoom> createContactRoom(@RequestBody CreateContactRoomRequest request) {
        Long currentUserId = getCurrentUserId();
        ChatRoom contactRoom = chatRoomService.createContactChatRoom(
                currentUserId,
                request.getTargetUserId(),
                request.getBoardType(),
                request.getBoardId(),
                request.getBoardTitle()
        );
        return ResponseEntity.ok(contactRoom);
    }

    private ChatRoomListResponse buildRoomListResponse(ChatRoom room, Long userId) {
        String lastMessage = chatMessageService.getLastMessageText(room.getRoomId());
        int unreadCount = chatService.getUnreadMessageCount(room.getRoomId(), userId);

        return ChatRoomListResponse.builder()
                .roomId(room.getRoomId())
                .title(room.getTitle())
                .participantCount(room.getParticipants().size())
                .lastMessage(lastMessage)
                .unreadCount(unreadCount)
                .lastActivityAt(room.getLastActivityAt())
                .build();
    }

    private Long getCurrentUserId() {
        Member member = memberService.getMemberReferenceByContext();
        return member.getId();
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