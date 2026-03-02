package com.dongsoop.dongsoop.chat.controller;

import com.dongsoop.dongsoop.chat.dto.*;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomInitResponse;
import com.dongsoop.dongsoop.chat.service.ChatParticipantService;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.role.entity.RoleType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {
    private final ChatService chatService;
    private final ChatRoomService chatRoomService;
    private final ChatParticipantService chatParticipantService;
    private final ChatValidator chatValidator;
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
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/room/{roomId}/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        int unreadCount = chatService.getUnreadMessageCount(roomId, currentUserId);
        Map<String, Integer> response = createUnreadCountResponse(unreadCount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/room")
    public ResponseEntity<ChatRoom> createRoom(@RequestBody @Valid CreateRoomRequest request) {
        Long currentUserId = getCurrentUserId();
        Long targetUserId = request.getTargetUserId();
        ChatRoom createdRoom = chatRoomService.createOneToOneChatRoom(currentUserId, targetUserId, request.getTitle());
        return ResponseEntity.ok(createdRoom);
    }

    @PostMapping("/room/admin")
    @Secured(RoleType.ADMIN_ROLE)
    public ResponseEntity<ChatRoom> createRoom(@RequestBody @Valid CreateChatRoomByAdminRequest request) {

        ChatRoom createdRoom = chatRoomService.createContactChatRoom(
                request.sourceUserId(),
                request.targetUserId(),
                request.boardType(),
                request.boardId(),
                request.boardTitle()
        );

        return ResponseEntity.ok(createdRoom);
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomListResponse>> getRoomsForUser() {
        Long currentUserId = getCurrentUserId();
        List<ChatRoom> rooms = chatRoomService.getRoomsForUserId(currentUserId);
        List<ChatRoomListResponse> roomResponses = chatService.buildRoomListResponses(rooms, currentUserId);
        return ResponseEntity.ok(roomResponses);
    }

    @PostMapping("/room/group")
    public ResponseEntity<ChatRoom> createGroupRoom(@RequestBody @Valid CreateGroupRoomRequest request) {
        Long currentUserId = getCurrentUserId();
        ChatRoom groupRoom = chatRoomService.createGroupChatRoom(currentUserId, request.getParticipants(),
                request.getTitle());
        return ResponseEntity.ok(groupRoom);
    }

    @PostMapping("/room/{roomId}/invite")
    public ResponseEntity<ChatMessage> inviteUserToRoom(
            @PathVariable("roomId") String roomId,
            @RequestBody @Valid InviteUserRequest request) {
        Long currentUserId = getCurrentUserId();
        Long targetUserId = request.targetUserId();

        // ChatParticipantService 직접 호출
        ChatMessage inviteMessage = chatParticipantService.inviteUserToGroupChat(roomId, currentUserId, targetUserId);
        return ResponseEntity.ok(inviteMessage);
    }

    @PostMapping("/room/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable("roomId") String roomId) {
        Long currentUserId = getCurrentUserId();
        chatService.leaveChatRoom(roomId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/room/{roomId}/kick")
    public ResponseEntity<ChatRoom> kickUser(
            @PathVariable("roomId") String roomId,
            @RequestBody @Valid KickUserRequest request) {
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
        return ResponseEntity.noContent().build();
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
        // ChatValidator 직접 호출
        chatValidator.validateUserForRoom(roomId, currentUserId);

        ChatRoom room = chatRoomService.getChatRoomById(roomId);

        Map<Long, String> participants = room.getParticipants().stream()
                .collect(Collectors.toMap(
                        participantId -> participantId,
                        memberService::getNicknameById
                ));

        return ResponseEntity.ok(participants);
    }

    @PostMapping("/room/contact")
    public ResponseEntity<ChatRoom> createContactRoom(@RequestBody @Valid CreateContactRoomRequest request) {
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
