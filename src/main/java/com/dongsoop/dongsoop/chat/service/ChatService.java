package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.dto.ReadStatusUpdateRequest;
import com.dongsoop.dongsoop.chat.entity.*;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.exception.domain.websocket.UnauthorizedChatAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class ChatService {
    private final RedisChatRepository redisChatRepository;
    private final ChatValidator chatValidator;
    private final ChatSyncService chatSyncService;
    private final ReadStatusService readStatusService;

    public ChatService(RedisChatRepository redisChatRepository,
                       ChatValidator chatValidator,
                       ChatSyncService chatSyncService,
                       ReadStatusService readStatusService) {
        this.redisChatRepository = redisChatRepository;
        this.chatValidator = chatValidator;
        this.chatSyncService = chatSyncService;
        this.readStatusService = readStatusService;
    }

    public ChatRoomInitResponse initializeChatRoomForFirstTime(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        ChatRoom room = getChatRoomById(roomId);
        LocalDateTime userJoinTime = determineUserJoinTime(room, userId);
        List<ChatMessage> afterJoinMessages = loadMessagesAfterJoinTime(roomId, userJoinTime);

        readStatusService.initializeUserReadStatus(userId, roomId, userJoinTime);

        return buildChatRoomInitResponse(room, afterJoinMessages, userJoinTime);
    }

    public IncrementalSyncResponse syncNewMessagesOnly(String roomId, Long userId, String lastMessageId) {
        chatValidator.validateUserForRoom(roomId, userId);

        List<ChatMessage> newMessages = loadNewMessages(roomId, lastMessageId);
        int unreadCount = countUnreadMessages(newMessages, userId);

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

    public ChatRoom createOneToOneChatRoom(Long userId, Long targetUserId) {
        validateOneToOneChatCreation(userId, targetUserId);
        return findExistingRoomOrCreate(userId, targetUserId);
    }

    public ChatRoom createGroupChatRoom(Long creatorId, Set<Long> participants, String title) {
        validateGroupChatCreation(participants);

        ChatRoom room = createGroupRoom(participants, creatorId, title);
        return saveRoom(room);
    }

    public void enterChatRoom(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
    }

    public void leaveChatRoom(String roomId, Long userId) {
        ChatRoom room = getChatRoomById(roomId);

        processUserLeaveWithMessage(room, roomId, userId);
        saveRoom(room);
        deleteRoomIfEmpty(room);
    }

    public ChatMessage processMessage(ChatMessage message) {
        ChatMessage enrichedMessage = chatValidator.validateAndEnrichMessage(message);
        saveMessage(enrichedMessage);
        updateRoomActivity(enrichedMessage.getRoomId());

        return enrichedMessage;
    }

    public ChatMessage processWebSocketMessage(ChatMessage message, Long userId, String roomId) {
        ChatMessage enrichedMessage = enrichMessageWithUserData(message, userId, roomId);
        return processMessage(enrichedMessage);
    }

    public ChatMessage processWebSocketEnter(String roomId, Long userId) {
        return checkFirstTimeEntryAndCreateEnterMessage(roomId, userId);
    }

    public List<ChatMessage> getChatHistoryForUser(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        ChatRoom room = getChatRoomById(roomId);
        LocalDateTime userJoinTime = determineUserJoinTime(room, userId);

        return loadMessagesAfterJoinTime(roomId, userJoinTime);
    }

    public List<ChatMessage> getMessagesAfter(String roomId, Long userId, String messageId) {
        chatValidator.validateUserForRoom(roomId, userId);
        return redisChatRepository.findMessagesByRoomIdAfterId(roomId, messageId);
    }

    public void markAllMessagesAsRead(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        updateReadTimestamp(userId, roomId, LocalDateTime.now());
    }

    public List<ChatMessage> syncOfflineMessages(String roomId, Long userId, List<ChatMessage> offlineMessages) {
        chatValidator.validateUserForRoom(roomId, userId);
        return processOfflineMessages(roomId, userId, offlineMessages);
    }

    public ChatRoom getChatRoomById(String roomId) {
        return chatSyncService.findRoomOrRestore(roomId);
    }

    public ChatRoom kickUserFromRoom(String roomId, Long managerId, Long userToKick) {
        ChatRoom room = getChatRoomById(roomId);

        chatValidator.validateManagerPermission(room, managerId);
        chatValidator.validateKickableUser(room, userToKick);

        processUserKickWithMessage(room, roomId, userToKick);

        return saveRoom(room);
    }

    public List<ChatRoom> getRoomsForUserId(Long userId) {
        List<ChatRoom> allRooms = redisChatRepository.findRoomsByUserId(userId);
        return filterNotKickedRooms(allRooms, userId);
    }

    private ChatRoomInitResponse buildChatRoomInitResponse(ChatRoom room, List<ChatMessage> messages, LocalDateTime userJoinTime) {
        return ChatRoomInitResponse.builder()
                .room(room)
                .messages(messages)
                .userJoinTime(userJoinTime)
                .totalMessageCount(messages.size())
                .build();
    }

    private List<ChatMessage> loadMessagesAfterJoinTime(String roomId, LocalDateTime userJoinTime) {
        return redisChatRepository.findMessagesByRoomIdAfterTime(roomId, userJoinTime);
    }

    private List<ChatMessage> loadNewMessages(String roomId, String lastMessageId) {
        if (lastMessageId == null) {
            return redisChatRepository.findMessagesByRoomId(roomId);
        }
        return redisChatRepository.findMessagesByRoomIdAfterId(roomId, lastMessageId);
    }

    private int countUnreadMessages(List<ChatMessage> messages, Long userId) {
        return ChatMessage.countUnreadMessages(messages, userId);
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
        List<ChatMessage> messages = redisChatRepository.findMessagesByRoomId(roomId);

        ChatMessage targetMessage = findMessageById(messages, messageId);
        if (targetMessage != null) {
            updateReadTimestamp(userId, roomId, targetMessage.getTimestamp());
        }
    }

    private ChatMessage findMessageById(List<ChatMessage> messages, String messageId) {
        return messages.stream()
                .filter(msg -> msg.getMessageId().equals(messageId))
                .findFirst()
                .orElse(null);
    }

    private int calculateUnreadCount(String roomId, Long userId, LocalDateTime lastReadTime) {
        if (lastReadTime == null) {
            return 0;
        }

        List<ChatMessage> unreadMessages = redisChatRepository.findMessagesByRoomIdAfterTime(roomId, lastReadTime);
        return ChatMessage.countUnreadMessages(unreadMessages, userId);
    }

    private void validateOneToOneChatCreation(Long userId, Long targetUserId) {
        chatValidator.validateSelfChat(userId, targetUserId);
        validatePositiveUserId(userId);
        validatePositiveUserId(targetUserId);
    }

    private void validateGroupChatCreation(Set<Long> participants) {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("그룹 채팅 참여자 수가 올바르지 않습니다.");
        }
    }

    private ChatRoom findExistingRoomOrCreate(Long userId, Long targetUserId) {
        ChatRoom existingRoom = redisChatRepository.findRoomByParticipants(userId, targetUserId).orElse(null);

        if (existingRoom != null) {
            return existingRoom;
        }
        return createNewOneToOneRoom(userId, targetUserId);
    }

    private ChatRoom createNewOneToOneRoom(Long userId, Long targetUserId) {
        ChatRoom room = ChatRoom.create(userId, targetUserId);
        return saveRoom(room);
    }

    private ChatRoom createGroupRoom(Set<Long> participants, Long creatorId, String title) {
        return ChatRoom.createWithParticipantsAndTitle(participants, creatorId, title);
    }

    private ChatRoom saveRoom(ChatRoom room) {
        return redisChatRepository.saveRoom(room);
    }

    private LocalDateTime determineUserJoinTime(ChatRoom room, Long userId) {
        LocalDateTime existingJoinTime = room.getJoinTime(userId);

        if (existingJoinTime != null) {
            return existingJoinTime;
        }
        return addNewParticipantAndGetJoinTime(room, userId);
    }

    private LocalDateTime addNewParticipantAndGetJoinTime(ChatRoom room, Long userId) {
        LocalDateTime joinTime = LocalDateTime.now();
        room.addNewParticipant(userId);
        saveRoom(room);
        return joinTime;
    }

    private ChatMessage checkFirstTimeEntryAndCreateEnterMessage(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        if (isFirstTimeEntry(roomId, userId)) {
            return createAndSaveSystemMessage(roomId, userId, MessageType.ENTER);
        }
        return null;
    }

    private boolean isFirstTimeEntry(String roomId, Long userId) {
        ChatRoom room = getChatRoomById(roomId);
        LocalDateTime userJoinTime = room.getJoinTime(userId);

        if (userJoinTime == null) {
            return true;
        }
        return !hasUserEnteredBefore(roomId, userId, userJoinTime);
    }

    private boolean hasUserEnteredBefore(String roomId, Long userId, LocalDateTime joinTime) {
        List<ChatMessage> enterMessages = redisChatRepository.findMessagesByRoomIdAfterTime(roomId, joinTime)
                .stream()
                .filter(msg -> msg.getType() == MessageType.ENTER)
                .filter(msg -> msg.getSenderId().equals(userId))
                .toList();

        return !enterMessages.isEmpty();
    }

    private void processUserLeaveWithMessage(ChatRoom room, String roomId, Long userId) {
        room.kickUser(userId);
        createAndSaveSystemMessage(roomId, userId, MessageType.LEAVE);
    }

    private void processUserKickWithMessage(ChatRoom room, String roomId, Long userToKick) {
        room.kickUser(userToKick);
        createAndSaveSystemMessage(roomId, userToKick, MessageType.LEAVE);
    }

    private List<ChatRoom> filterNotKickedRooms(List<ChatRoom> allRooms, Long userId) {
        return allRooms.stream()
                .filter(room -> !room.isKicked(userId))
                .toList();
    }

    private void deleteRoomIfEmpty(ChatRoom room) {
        if (room.getParticipants().isEmpty()) {
            deleteRoom(room.getRoomId());
        }
    }

    private void deleteRoom(String roomId) {
        redisChatRepository.deleteRoom(roomId);
    }

    private void saveMessage(ChatMessage message) {
        redisChatRepository.saveMessage(message);
    }

    private ChatMessage createAndSaveSystemMessage(String roomId, Long userId, MessageType type) {
        ChatMessage message = buildSystemMessage(roomId, userId, type);
        saveMessage(message);
        return message;
    }

    private ChatMessage buildSystemMessage(String roomId, Long userId, MessageType type) {
        return ChatMessage.builder()
                .messageId(generateUniqueMessageId())
                .roomId(roomId)
                .senderId(userId)
                .content(createSystemMessageContent(userId))
                .timestamp(getCurrentTimestamp())
                .type(type)
                .build();
    }

    private ChatMessage enrichMessageWithUserData(ChatMessage message, Long userId, String roomId) {
        message.setSenderId(userId);
        message.setRoomId(roomId);
        return message;
    }

    private List<ChatMessage> processOfflineMessages(String roomId, Long userId, List<ChatMessage> offlineMessages) {
        return offlineMessages.stream()
                .map(message -> processOfflineMessage(message, userId, roomId))
                .filter(Objects::nonNull)
                .toList();
    }

    private ChatMessage processOfflineMessage(ChatMessage message, Long userId, String roomId) {
        if (message == null) {
            return null;
        }

        ChatMessage enrichedMessage = enrichOfflineMessage(message, userId, roomId);
        return validateAndSaveOfflineMessage(enrichedMessage);
    }

    private ChatMessage enrichOfflineMessage(ChatMessage message, Long userId, String roomId) {
        message.setSenderId(userId);
        message.setRoomId(roomId);

        enrichOfflineMessageId(message);
        enrichOfflineMessageTimestamp(message);
        enrichOfflineMessageType(message);

        return message;
    }

    private void enrichOfflineMessageId(ChatMessage message) {
        if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
            message.setMessageId(generateUniqueMessageId());
        }
    }

    private void enrichOfflineMessageTimestamp(ChatMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(getCurrentTimestamp());
        }
    }

    private void enrichOfflineMessageType(ChatMessage message) {
        if (message.getType() == null) {
            message.setType(MessageType.CHAT);
        }
    }

    private ChatMessage validateAndSaveOfflineMessage(ChatMessage message) {
        saveMessage(message);
        updateRoomActivity(message.getRoomId());
        return message;
    }

    private void validatePositiveUserId(Long userId) {
        if (userId < 0) {
            throw new UnauthorizedChatAccessException();
        }
    }

    private void updateRoomActivity(String roomId) {
        ChatRoom room = getChatRoomById(roomId);
        room.updateActivity();
        saveRoom(room);
    }

    private String generateUniqueMessageId() {
        return UUID.randomUUID().toString();
    }

    private String createSystemMessageContent(Long userId) {
        return userId.toString();
    }

    private LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }
}