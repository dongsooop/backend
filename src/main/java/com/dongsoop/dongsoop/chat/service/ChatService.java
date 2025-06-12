package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.dto.ReadStatusUpdateRequest;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomInitResponse;
import com.dongsoop.dongsoop.chat.entity.IncrementalSyncResponse;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.exception.domain.websocket.UnauthorizedChatAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
        List<ChatMessage> allMessages = loadMessagesAfterJoinTime(roomId, userJoinTime);

        initializeReadStatus(userId, roomId, userJoinTime);

        return buildInitResponse(room, allMessages, userJoinTime);
    }

    public IncrementalSyncResponse syncNewMessagesOnly(String roomId, Long userId, String lastMessageId) {
        chatValidator.validateUserForRoom(roomId, userId);

        List<ChatMessage> newMessages = loadNewMessages(roomId, lastMessageId);
        int unreadCount = calculateUnreadCountForUser(newMessages, userId);

        return IncrementalSyncResponse.create(roomId, newMessages, unreadCount);
    }

    public void updateReadStatus(String roomId, Long userId, ReadStatusUpdateRequest request) {
        chatValidator.validateUserForRoom(roomId, userId);

        processReadTimeUpdate(userId, roomId, request.getReadUntilTime());
        processMessageIdReadUpdate(roomId, userId, request.getLastReadMessageId());
        processDefaultReadStatus(request, roomId, userId);
    }

    public int getUnreadMessageCount(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        LocalDateTime lastReadTime = readStatusService.getLastReadTimestamp(userId, roomId);

        return calculateUnreadCountFromTime(roomId, userId, lastReadTime);
    }

    public ChatRoom createOneToOneChatRoom(Long userId, Long targetUserId) {
        validateOneToOneChatParams(userId, targetUserId);

        return findExistingRoomOrCreate(userId, targetUserId);
    }

    public ChatRoom createGroupChatRoom(Long creatorId, Set<Long> participants, String title) {
        validateGroupChatParams(participants);

        ChatRoom room = createGroupRoom(participants, creatorId, title);
        return saveRoom(room);
    }

    public void enterChatRoom(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
    }

    public void leaveChatRoom(String roomId, Long userId) {
        ChatRoom room = getChatRoomById(roomId);

        processUserLeave(room, roomId, userId);
        saveRoom(room);
        deleteRoomIfEmpty(room);
    }

    public ChatMessage processMessage(ChatMessage message) {
        ChatMessage enrichedMessage = chatValidator.validateAndEnrichMessage(message);
        saveMessage(enrichedMessage);
        updateRoomActivity(enrichedMessage.getRoomId());

        return enrichedMessage;
    }

    public ChatMessage createEnterMessage(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        return createAndSaveSystemMessage(roomId, userId, MessageType.ENTER);
    }

    public ChatMessage processWebSocketMessage(ChatMessage message, Long userId, String roomId) {
        ChatMessage enrichedMessage = enrichMessageWithUserData(message, userId, roomId);
        return processMessage(enrichedMessage);
    }

    public ChatMessage processWebSocketEnter(String roomId, Long userId) {
        return createEnterMessage(roomId, userId);
    }

    public List<ChatMessage> getChatHistoryForUser(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        ChatRoom room = getChatRoomById(roomId);
        LocalDateTime userJoinTime = determineUserJoinTime(room, userId);

        List<ChatMessage> messages = loadMessagesAfterJoinTime(roomId, userJoinTime);

        return messages;
    }

    public List<ChatMessage> getMessagesAfter(String roomId, Long userId, String messageId) {
        chatValidator.validateUserForRoom(roomId, userId);

        List<ChatMessage> messages = redisChatRepository.findMessagesByRoomIdAfterId(roomId, messageId);

        return messages;
    }

    // 읽음 상태 관리
    public void markAllMessagesAsRead(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        updateReadTimestamp(userId, roomId, LocalDateTime.now());
    }

    // 오프라인 메시지 동기화
    public List<ChatMessage> syncOfflineMessages(String roomId, Long userId, List<ChatMessage> offlineMessages) {
        chatValidator.validateUserForRoom(roomId, userId);

        List<ChatMessage> processedMessages = processOfflineMessages(roomId, userId, offlineMessages);

        return processedMessages;
    }

    // 채팅방 및 방 목록 조회
    public ChatRoom getChatRoomById(String roomId) {
        return chatSyncService.findRoomOrRestore(roomId);
    }

    public List<ChatRoom> getRoomsForUserId(Long userId) {
        List<ChatRoom> allRooms = redisChatRepository.findRoomsByUserId(userId);
        return filterNotKickedRooms(allRooms, userId);
    }

    private void initializeReadStatus(Long userId, String roomId, LocalDateTime userJoinTime) {
        readStatusService.initializeUserReadStatus(userId, roomId, userJoinTime);
    }

    private List<ChatMessage> loadMessagesAfterJoinTime(String roomId, LocalDateTime userJoinTime) {
        return redisChatRepository.findMessagesByRoomIdAfterTime(roomId, userJoinTime);
    }

    private ChatRoomInitResponse buildInitResponse(ChatRoom room, List<ChatMessage> messages, LocalDateTime userJoinTime) {
        return ChatRoomInitResponse.builder()
                .room(room)
                .messages(messages)
                .userJoinTime(userJoinTime)
                .totalMessageCount(messages.size())
                .build();
    }

    private List<ChatMessage> loadNewMessages(String roomId, String lastMessageId) {
        return Optional.ofNullable(lastMessageId)
                .map(id -> redisChatRepository.findMessagesByRoomIdAfterId(roomId, id))
                .orElseGet(() -> redisChatRepository.findMessagesByRoomId(roomId));
    }

    private int calculateUnreadCountForUser(List<ChatMessage> messages, Long userId) {
        return ChatMessage.countUnreadMessages(messages, userId);
    }

    private void processReadTimeUpdate(Long userId, String roomId, LocalDateTime readUntilTime) {
        Optional.ofNullable(readUntilTime)
                .ifPresent(time -> updateReadTimestamp(userId, roomId, time));
    }

    private void processMessageIdReadUpdate(String roomId, Long userId, String lastReadMessageId) {
        Optional.ofNullable(lastReadMessageId)
                .ifPresent(messageId -> updateReadStatusByMessageId(roomId, userId, messageId));
    }

    private void processDefaultReadStatus(ReadStatusUpdateRequest request, String roomId, Long userId) {
        boolean hasNoRequest = request.getReadUntilTime() == null && request.getLastReadMessageId() == null;

        Optional.of(hasNoRequest)
                .filter(Boolean::booleanValue)
                .ifPresent(unused -> updateReadTimestamp(userId, roomId, LocalDateTime.now()));
    }

    private void updateReadTimestamp(Long userId, String roomId, LocalDateTime timestamp) {
        readStatusService.updateLastReadTimestamp(userId, roomId, timestamp);
    }

    private void updateReadStatusByMessageId(String roomId, Long userId, String messageId) {
        List<ChatMessage> messages = redisChatRepository.findMessagesByRoomId(roomId);

        messages.stream()
                .filter(msg -> msg.getMessageId().equals(messageId))
                .findFirst()
                .ifPresent(msg -> updateReadTimestamp(userId, roomId, msg.getTimestamp()));
    }

    private int calculateUnreadCountFromTime(String roomId, Long userId, LocalDateTime lastReadTime) {
        return Optional.ofNullable(lastReadTime)
                .map(time -> calculateUnreadCount(roomId, userId, time))
                .orElse(0);
    }

    private int calculateUnreadCount(String roomId, Long userId, LocalDateTime lastReadTime) {
        List<ChatMessage> unreadMessages = redisChatRepository.findMessagesByRoomIdAfterTime(roomId, lastReadTime);
        return ChatMessage.countUnreadMessages(unreadMessages, userId);
    }

    private void validateOneToOneChatParams(Long userId, Long targetUserId) {
        chatValidator.validateSelfChat(userId, targetUserId);
        validatePositiveUserId(userId);
        validatePositiveUserId(targetUserId);
    }

    private void validateGroupChatParams(Set<Long> participants) {
        Optional.of(participants)
                .filter(p -> !p.isEmpty())
                .orElseThrow(() -> new IllegalArgumentException("그룹 채팅 참여자 수가 올바르지 않습니다."));
    }

    private ChatRoom findExistingRoomOrCreate(Long userId, Long targetUserId) {
        ChatRoom existingRoom = redisChatRepository.findRoomByParticipants(userId, targetUserId).orElse(null);

        return Optional.ofNullable(existingRoom)
                .orElseGet(() -> createNewOneToOneRoom(userId, targetUserId));
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

        return Optional.ofNullable(existingJoinTime)
                .orElseGet(() -> addNewParticipantAndGetJoinTime(room, userId));
    }

    private LocalDateTime addNewParticipantAndGetJoinTime(ChatRoom room, Long userId) {
        LocalDateTime joinTime = LocalDateTime.now();
        room.addNewParticipant(userId);
        saveRoom(room);
        return joinTime;
    }

    private void processUserLeave(ChatRoom room, String roomId, Long userId) {
        room.kickUser(userId);
        createAndSaveSystemMessage(roomId, userId, MessageType.LEAVE);
    }

    private List<ChatRoom> filterNotKickedRooms(List<ChatRoom> allRooms, Long userId) {
        return allRooms.stream()
                .filter(room -> !room.isKicked(userId))
                .toList();
    }

    private void deleteRoomIfEmpty(ChatRoom room) {
        Optional.of(room)
                .filter(r -> r.getParticipants().isEmpty())
                .ifPresent(r -> deleteRoom(r.getRoomId()));
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
        return Optional.ofNullable(message)
                .map(msg -> enrichOfflineMessage(msg, userId, roomId))
                .map(this::validateAndSaveOfflineMessage)
                .orElse(null);
    }

    private ChatMessage enrichOfflineMessage(ChatMessage message, Long userId, String roomId) {
        message.setSenderId(userId);
        message.setRoomId(roomId);

        enrichMessageId(message);
        enrichMessageTimestamp(message);
        enrichMessageType(message);

        return message;
    }

    private void enrichMessageId(ChatMessage message) {
        Optional.ofNullable(message.getMessageId())
                .filter(id -> id.isEmpty())
                .ifPresent(empty -> message.setMessageId(generateUniqueMessageId()));
    }

    private void enrichMessageTimestamp(ChatMessage message) {
        Optional.ofNullable(message.getTimestamp())
                .orElseGet(() -> {
                    message.setTimestamp(getCurrentTimestamp());
                    return message.getTimestamp();
                });
    }

    private void enrichMessageType(ChatMessage message) {
        Optional.ofNullable(message.getType())
                .orElseGet(() -> {
                    message.setType(MessageType.CHAT);
                    return message.getType();
                });
    }

    private ChatMessage validateAndSaveOfflineMessage(ChatMessage message) {
        return Optional.ofNullable(message)
                .map(msg -> {
                    saveMessage(msg);
                    updateRoomActivity(msg.getRoomId());
                    return msg;
                })
                .orElse(null);
    }

    private void validatePositiveUserId(Long userId) {
        Optional.of(userId)
                .filter(id -> id >= 0)
                .orElseThrow(UnauthorizedChatAccessException::new);
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