package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.dto.BlockStatusMessage;
import com.dongsoop.dongsoop.chat.dto.ChatRoomListResponse;
import com.dongsoop.dongsoop.chat.dto.ChatRoomUpdateDto;
import com.dongsoop.dongsoop.chat.dto.ReadStatusUpdateRequest;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatNotificationType;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomInitResponse;
import com.dongsoop.dongsoop.chat.entity.ChatRoomType;
import com.dongsoop.dongsoop.chat.notification.ChatNotification;
import com.dongsoop.dongsoop.chat.session.WebSocketSessionManager;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberblock.constant.BlockStatus;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatParticipantService chatParticipantService;
    private final ReadStatusService readStatusService;
    private final ChatValidator chatValidator;
    private final MemberBlockRepository memberBlockRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatNotification chatNotification;
    private final MemberService memberService;
    private final WebSocketSessionManager sessionManager;

    public ChatRoomInitResponse initializeChatRoomForFirstTime(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        ChatRoom room = chatRoomService.getChatRoomById(roomId);
        LocalDateTime userJoinTime = chatParticipantService.determineUserJoinTime(room, userId);
        List<ChatMessage> afterJoinMessages = chatMessageService.loadMessagesAfterJoinTime(roomId, userJoinTime);

        readStatusService.initializeUserReadStatus(userId, roomId, userJoinTime);

        return buildChatRoomInitResponse(room, afterJoinMessages, userJoinTime);
    }

    public void updateReadStatus(String roomId, Long userId, ReadStatusUpdateRequest request) {
        chatValidator.validateUserForRoom(roomId, userId);
        processReadStatusUpdate(userId, roomId, request);
    }

    public int getUnreadMessageCount(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        LocalDateTime lastReadTime = readStatusService.getLastReadTimestamp(userId, roomId);
        return calculateUnreadCount(roomId, userId, lastReadTime);
    }

    public void leaveChatRoom(String roomId, Long userId) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);

        chatValidator.validateUserForRoom(roomId, userId);
        chatValidator.validateManagerCanLeave(room, userId);

        chatParticipantService.leaveChatRoom(roomId, userId);
    }

    public ChatMessage processWebSocketMessage(ChatMessage message, Long userId, String roomId) {
        ChatMessage processedMessage = chatMessageService.processWebSocketMessage(message, userId, roomId);
        ChatRoom room = chatRoomService.updateRoomActivity(roomId);

        Set<Long> receiver = new HashSet<>(room.getParticipants());
        receiver.remove(userId);

        sendFcmNotification(receiver, roomId, userId, message);
        sendGlobalRoomUpdate(room.getParticipants(), roomId, processedMessage);

        return processedMessage;
    }

    private void sendFcmNotification(Set<Long> receiver, String roomId, Long userId, ChatMessage message) {
        if (receiver == null || receiver.isEmpty()) {
            return;
        }

        Set<Long> offlineReceivers = receiver.stream()
                .filter(id -> !sessionManager.isUserOnline(id))
                .collect(Collectors.toSet());

        if (offlineReceivers.isEmpty()) {
            return;
        }

        String senderName = memberService.getNicknameById(userId);
        chatNotification.send(offlineReceivers, roomId, senderName, message.getContent());
    }

    private void sendGlobalRoomUpdate(Set<Long> participants, String roomId, ChatMessage message) {
        List<Long> participantList = new ArrayList<>(participants);
        Map<Long, LocalDateTime> lastReadTimestamps =
                readStatusService.getLastReadTimestampsBatchForUsers(participantList, roomId);

        for (Long participantId : participantList) {
            LocalDateTime lastReadTime = lastReadTimestamps.get(participantId);
            int unreadCount = calculateUnreadCount(roomId, participantId, lastReadTime);
            ChatRoomUpdateDto updateDto = ChatRoomUpdateDto.createRoomUpdate(roomId, message, unreadCount);
            messagingTemplate.convertAndSend("/topic/user/" + participantId, updateDto);
        }
    }

    public ChatMessage processWebSocketEnter(String roomId, Long userId) {
        return chatParticipantService.checkFirstTimeEntryAndCreateEnterMessage(roomId, userId);
    }

    public List<ChatMessage> getMessagesAfter(String roomId, Long userId, String messageId) {
        chatValidator.validateUserForRoom(roomId, userId);
        return chatMessageService.getMessagesAfterId(roomId, messageId);
    }

    public void markAllMessagesAsRead(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        updateReadTimestamp(userId, roomId, LocalDateTime.now());

        notifyReadStatusChanged(roomId, userId);
    }

    public List<ChatRoomListResponse> buildRoomListResponses(List<ChatRoom> rooms, Long userId) {
        List<String> roomIds = rooms.stream()
                .map(ChatRoom::getRoomId)
                .toList();

        Map<String, LocalDateTime> lastReadTimestamps =
                readStatusService.getLastReadTimestampsBatch(userId, roomIds);
        Map<String, String> lastMessages =
                chatMessageService.getLastMessageTextsBatch(roomIds);

        return rooms.stream()
                .map(room -> {
                    String roomId = room.getRoomId();
                    LocalDateTime lastReadTime = lastReadTimestamps.get(roomId);
                    int unreadCount = calculateUnreadCount(roomId, userId, lastReadTime);
                    String lastMessage = lastMessages.get(roomId);

                    return ChatRoomListResponse.builder()
                            .roomId(roomId)
                            .title(room.getTitle())
                            .participantCount(room.getParticipants().size())
                            .lastMessage(lastMessage)
                            .unreadCount(unreadCount)
                            .lastActivityAt(room.getLastActivityAt())
                            .isGroupChat(room.isGroupChat())
                            .roomType(determineRoomType(room))
                            .build();
                })
                .toList();
    }

    private void notifyReadStatusChanged(String roomId, Long readerId) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);
        Set<Long> otherParticipants = new HashSet<>(room.getParticipants());
        otherParticipants.remove(readerId);

        for (Long participantId : otherParticipants) {
            sendReadStatusUpdateNotification(participantId, roomId, readerId);
        }
    }

    private void sendReadStatusUpdateNotification(Long userId, String roomId, Long readerId) {
        Map<String, Object> readUpdate = Map.of(
                "type", ChatNotificationType.READ_STATUS_UPDATE,
                "roomId", roomId,
                "readerId", readerId,
                "timestamp", LocalDateTime.now()
        );

        messagingTemplate.convertAndSend("/topic/user/" + userId, readUpdate);
    }

    public List<ChatMessage> syncOfflineMessages(String roomId, Long userId, List<ChatMessage> offlineMessages) {
        chatValidator.validateUserForRoom(roomId, userId);
        return chatMessageService.processOfflineMessages(roomId, userId, offlineMessages);
    }

    public ChatRoom kickUserFromRoom(String roomId, Long managerId, Long userToKick) {
        return chatParticipantService.kickUserFromRoom(roomId, managerId, userToKick);
    }

    public List<ChatRoom> getRoomsForUserId(Long userId) {
        return chatRoomService.getRoomsForUserId(userId);
    }

    private ChatRoomInitResponse buildChatRoomInitResponse(ChatRoom room, List<ChatMessage> messages,
                                                           LocalDateTime userJoinTime) {
        return ChatRoomInitResponse.builder()
                .room(room)
                .messages(messages)
                .userJoinTime(userJoinTime)
                .totalMessageCount(messages.size())
                .build();
    }

    private void processReadStatusUpdate(Long userId, String roomId, ReadStatusUpdateRequest request) {
        processReadTimeUpdate(userId, roomId, request.getReadUntilTime());
        processMessageIdReadUpdate(roomId, userId, request.getLastReadMessageId());
        processDefaultReadStatus(request, roomId, userId);
    }

    private void processReadTimeUpdate(Long userId, String roomId, LocalDateTime readUntilTime) {
        if (readUntilTime != null) {
            updateReadTimestamp(userId, roomId, readUntilTime);
        }
    }

    private void processMessageIdReadUpdate(String roomId, Long userId, String lastReadMessageId) {
        if (lastReadMessageId != null) {
            updateReadStatusByMessageId(roomId, userId, lastReadMessageId);
        }
    }

    private void processDefaultReadStatus(ReadStatusUpdateRequest request, String roomId, Long userId) {
        if (hasNoReadStatusRequest(request)) {
            updateReadTimestamp(userId, roomId, LocalDateTime.now());
        }
    }

    private boolean hasNoReadStatusRequest(ReadStatusUpdateRequest request) {
        return request.getReadUntilTime() == null && request.getLastReadMessageId() == null;
    }

    private void updateReadTimestamp(Long userId, String roomId, LocalDateTime timestamp) {
        readStatusService.updateLastReadTimestamp(userId, roomId, timestamp);
    }

    private void updateReadStatusByMessageId(String roomId, Long userId, String messageId) {
        List<ChatMessage> messages = chatMessageService.getAllMessages(roomId);

        ChatMessage targetMessage = chatMessageService.findMessageById(messages, messageId);
        if (targetMessage != null) {
            updateReadTimestamp(userId, roomId, targetMessage.getTimestamp());
        }
    }

    // 마지막 읽음 시간 이후 안 읽은 메시지 수 계산
    private int calculateUnreadCount(String roomId, Long userId, LocalDateTime lastReadTime) {
        if (lastReadTime == null) {
            return 0;
        }

        List<ChatMessage> unreadMessages = chatMessageService.loadMessagesAfterJoinTime(roomId, lastReadTime);
        return chatMessageService.countUnreadMessages(unreadMessages, userId);
    }

    // 채팅방 유형 판별
    private String determineRoomType(ChatRoom room) {
        if (room.getTitle() != null && room.getTitle().startsWith("[문의]")) {
            return ChatRoomType.CONTACT.getValue();
        }
        if (room.isGroupChat()) {
            return ChatRoomType.GROUP.getValue();
        }
        return ChatRoomType.ONE_TO_ONE.getValue();
    }

    public BlockStatus getBlockStatus(String roomId, Long userId) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);

        if (room.isGroupChat()) {
            return BlockStatus.NONE;
        }
        Long otherUserId = findOtherUserId(room, userId);

        if (otherUserId == null) {
            return BlockStatus.NONE;
        }

        boolean iBlockedOther = memberBlockRepository.existsByBlockerIdAndBlockedId(userId, otherUserId);
        boolean otherBlockedMe = memberBlockRepository.existsByBlockerIdAndBlockedId(otherUserId, userId);

        if (iBlockedOther) {
            return BlockStatus.I_BLOCKED;
        }

        if (otherBlockedMe) {
            return BlockStatus.BLOCKED_BY_OTHER;
        }

        return BlockStatus.NONE;
    }

    private Long findOtherUserId(ChatRoom room, Long userId) {
        for (Long participantId : room.getParticipants()) {
            if (!participantId.equals(userId)) {
                return participantId;
            }
        }
        return null;
    }

    public void sendBlockStatusToUser(String roomId, Long userId, BlockStatus blockStatus) {
        BlockStatusMessage msg = new BlockStatusMessage(roomId, blockStatus.name());

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/topic/chat/room/" + roomId,
                msg
        );
    }
}
