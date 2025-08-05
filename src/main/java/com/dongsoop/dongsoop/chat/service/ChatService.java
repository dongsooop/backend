package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.dto.ChatRoomOverview;
import com.dongsoop.dongsoop.chat.dto.ReadStatusUpdateRequest;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomInitResponse;
import com.dongsoop.dongsoop.chat.entity.IncrementalSyncResponse;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.memberblock.constant.BlockType;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public ChatRoomInitResponse initializeChatRoomForFirstTime(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        ChatRoom room = chatRoomService.getChatRoomById(roomId);
        LocalDateTime userJoinTime = chatParticipantService.determineUserJoinTime(room, userId, chatRoomService);
        List<ChatMessage> afterJoinMessages = chatMessageService.loadMessagesAfterJoinTime(roomId, userJoinTime);

        readStatusService.initializeUserReadStatus(userId, roomId, userJoinTime);

        return buildChatRoomInitResponse(room, afterJoinMessages, userJoinTime);
    }

    public IncrementalSyncResponse syncNewMessagesOnly(String roomId, Long userId, String lastMessageId) {
        chatValidator.validateUserForRoom(roomId, userId);

        List<ChatMessage> newMessages = chatMessageService.loadNewMessages(roomId, lastMessageId);
        int unreadCount = chatMessageService.countUnreadMessages(newMessages, userId);

        return IncrementalSyncResponse.create(roomId, newMessages, unreadCount);
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

    public ChatRoom createGroupChatRoom(Long creatorId, Set<Long> participants, String title) {
        return chatRoomService.createGroupChatRoom(creatorId, participants, title);
    }

    public void leaveChatRoom(String roomId, Long userId) {
        chatParticipantService.leaveChatRoom(roomId, userId, chatRoomService, chatMessageService);
    }

    public ChatMessage processWebSocketMessage(ChatMessage message, Long userId, String roomId) {
        ChatMessage processedMessage = chatMessageService.processWebSocketMessage(message, userId, roomId);
        chatRoomService.updateRoomActivity(roomId);
        return processedMessage;
    }

    public ChatMessage processWebSocketEnter(String roomId, Long userId) {
        return chatParticipantService.checkFirstTimeEntryAndCreateEnterMessage(roomId, userId, chatRoomService,
                chatMessageService);
    }

    public List<ChatMessage> getChatHistoryForUser(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        ChatRoom room = chatRoomService.getChatRoomById(roomId);
        LocalDateTime userJoinTime = chatParticipantService.determineUserJoinTime(room, userId, chatRoomService);

        return chatMessageService.loadMessagesAfterJoinTime(roomId, userJoinTime);
    }

    public List<ChatMessage> getMessagesAfter(String roomId, Long userId, String messageId) {
        chatValidator.validateUserForRoom(roomId, userId);
        return chatMessageService.getMessagesAfterId(roomId, messageId);
    }

    public void markAllMessagesAsRead(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        updateReadTimestamp(userId, roomId, LocalDateTime.now());
    }

    public List<ChatMessage> syncOfflineMessages(String roomId, Long userId, List<ChatMessage> offlineMessages) {
        chatValidator.validateUserForRoom(roomId, userId);
        return chatMessageService.processOfflineMessages(roomId, userId, offlineMessages);
    }

    public ChatRoom kickUserFromRoom(String roomId, Long managerId, Long userToKick) {
        return chatParticipantService.kickUserFromRoom(roomId, managerId, userToKick, chatRoomService,
                chatMessageService);
    }

    public List<ChatRoomOverview> getRoomsForUserId(Long userId) {
        List<ChatRoom> rooms = chatRoomService.getRoomsForUserId(userId);
        return rooms.stream()
                .map(chatRoom -> transformToChatRoomOverview(chatRoom, userId))
                .toList();
    }

    /**
     * ChatRoom -> ChatRoomOverview 변환 과정으로 차단 내역 추가
     *
     * @param chatRoom 채팅방 정보
     * @param userId   요청자 ID
     * @return
     */
    private ChatRoomOverview transformToChatRoomOverview(ChatRoom chatRoom, Long userId) {
        if (chatRoom.isGroupChat()) {
            return new ChatRoomOverview(chatRoom, BlockType.NONE);
        }

        BlockType blockType = getBlockTypeByChatRoom(chatRoom, userId);

        return new ChatRoomOverview(chatRoom, blockType);
    }

    /**
     * 채팅방의 차단 상태를 확인하는 메서드
     *
     * @param chatRoom
     * @param userId
     * @return
     */
    private BlockType getBlockTypeByChatRoom(ChatRoom chatRoom, Long userId) {
        Long partnerId = getPartnerId(chatRoom, userId);
        if (partnerId == null) {
            return BlockType.NONE;
        }

        boolean isBlock = memberBlockRepository.existsByBlockerIdAndBlockedId(userId, partnerId);
        if (isBlock) {
            return BlockType.BLOCK;
        }

        boolean isBlocked = memberBlockRepository.existsByBlockerIdAndBlockedId(partnerId, userId);
        if (isBlocked) {
            return BlockType.BLOCKED;
        }

        return BlockType.NONE;
    }

    /**
     * 채팅방 상대방 ID를 가져오는 메서드
     *
     * @param chatRoom    채팅방 정보
     * @param requesterId 요청자 ID
     * @return
     */
    private Long getPartnerId(ChatRoom chatRoom, Long requesterId) {
        for (Long id : chatRoom.getParticipants()) {
            if (!id.equals(requesterId)) {
                return id;
            }
        }

        return null;
    }

    public ChatMessage inviteUserToGroupChat(String roomId, Long inviterId, Long targetUserId) {
        return chatParticipantService.inviteUserToGroupChat(roomId, inviterId, targetUserId, chatRoomService,
                chatMessageService);
    }

    public ChatMessage checkFirstTimeEntryAndCreateEnterMessage(String roomId, Long userId) {
        return chatParticipantService.checkFirstTimeEntryAndCreateEnterMessage(roomId, userId, chatRoomService,
                chatMessageService);
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

    private int calculateUnreadCount(String roomId, Long userId, LocalDateTime lastReadTime) {
        if (lastReadTime == null) {
            return 0;
        }

        List<ChatMessage> unreadMessages = chatMessageService.loadMessagesAfterJoinTime(roomId, lastReadTime);
        return chatMessageService.countUnreadMessages(unreadMessages, userId);
    }
}
