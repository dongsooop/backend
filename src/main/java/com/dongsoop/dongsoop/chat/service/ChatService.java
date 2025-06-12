package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.exception.domain.websocket.UnauthorizedChatAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatValidator chatValidator;
    private final ChatSyncService chatSyncService;

    public ChatService(@Qualifier("redisChatRepository") ChatRepository chatRepository,
                       ChatValidator chatValidator,
                       ChatSyncService chatSyncService
    ) {
        this.chatRepository = chatRepository;
        this.chatValidator = chatValidator;
        this.chatSyncService = chatSyncService;
    }

    public ChatRoom createOneToOneChatRoom(Long userId, Long targetUserId) {
        chatValidator.validateSelfChat(userId, targetUserId);
        validatePositiveUserId(userId);
        validatePositiveUserId(targetUserId);

        ChatRoom existingRoom = chatRepository.findRoomByParticipants(userId, targetUserId).orElse(null);

        if (existingRoom != null) {
            return existingRoom;
        }

        ChatRoom room = ChatRoom.create(userId, targetUserId);
        return chatRepository.saveRoom(room);
    }

    public ChatRoom createGroupChatRoom(Long creatorId, Set<Long> participants, String title) {
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("그룹 채팅 참여자 수가 올바르지 않습니다.");
        }

        ChatRoom room = ChatRoom.createWithParticipantsAndTitle(participants, creatorId, title);
        return chatRepository.saveRoom(room);
    }

    public ChatRoom kickUserFromRoom(String roomId, Long requesterId, Long userToKickId) {
        ChatRoom room = getChatRoomById(roomId);

        chatValidator.validateManagerPermission(room, requesterId);
        chatValidator.validateKickableUser(room, userToKickId);

        room.kickUser(userToKickId);
        createAndSaveSystemMessage(roomId, userToKickId, MessageType.LEAVE);

        return chatRepository.saveRoom(room);
    }

    public void enterChatRoom(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
    }

    public ChatMessage processMessage(ChatMessage message) {
        ChatMessage enrichedMessage = chatValidator.validateAndEnrichMessage(message);
        chatRepository.saveMessage(enrichedMessage);
        updateRoomActivity(enrichedMessage.getRoomId());
        return enrichedMessage;
    }

    public ChatMessage createEnterMessage(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        return createAndSaveSystemMessage(roomId, userId, MessageType.ENTER);
    }

    public List<ChatMessage> getChatHistory(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        return chatRepository.findMessagesByRoomId(roomId);
    }

    public List<ChatMessage> getMessagesSinceJoin(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        ChatRoom room = getChatRoomById(roomId);
        LocalDateTime joinTime = room.getJoinTime(userId);

        return Optional.ofNullable(joinTime)
                .map(time -> ((RedisChatRepository) chatRepository).findMessagesByRoomIdAfterTime(roomId, time))
                .orElse(List.of());
    }

    public List<ChatMessage> getMessagesAfter(String roomId, Long userId, String lastMessageId) {
        chatValidator.validateUserForRoom(roomId, userId);

        return ((RedisChatRepository) chatRepository).findMessagesByRoomIdAfterId(roomId, lastMessageId);
    }

    public ChatRoom getChatRoomById(String roomId) {
        return chatSyncService.findRoomOrRestore(roomId);
    }

    public List<ChatRoom> getRoomsForUserId(Long userId) {
        List<ChatRoom> allRooms = chatRepository.findRoomsByUserId(userId);
        return allRooms.stream()
                .filter(room -> !room.isKicked(userId))
                .toList();
    }

    public void leaveChatRoom(String roomId, Long userId) {
        ChatRoom room = getChatRoomById(roomId);

        room.kickUser(userId);
        createAndSaveSystemMessage(roomId, userId, MessageType.LEAVE);

        chatRepository.saveRoom(room);

        if (room.getParticipants().isEmpty()) {
            deleteRoom(roomId);
        }
    }

    public ChatMessage processWebSocketMessage(ChatMessage message, Long userId, String roomId) {
        ChatMessage enrichedMessage = enrichMessageWithUserData(message, userId, roomId);
        return processMessage(enrichedMessage);
    }

    public ChatMessage processWebSocketEnter(String roomId, Long userId) {
        return createEnterMessage(roomId, userId);
    }

    private void validatePositiveUserId(Long userId) {
        if (userId < 0) {
            throw new UnauthorizedChatAccessException();
        }
    }

    private ChatMessage createAndSaveSystemMessage(String roomId, Long userId, MessageType type) {
        ChatMessage message = buildSystemMessage(roomId, userId, type);
        chatRepository.saveMessage(message);
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

    private String generateUniqueMessageId() {
        return UUID.randomUUID().toString();
    }

    private String createSystemMessageContent(Long userId) {
        return userId.toString();
    }

    private LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }

    private ChatMessage enrichMessageWithUserData(ChatMessage message, Long userId, String roomId) {
        message.setSenderId(userId);
        message.setRoomId(roomId);
        return message;
    }

    private void updateRoomActivity(String roomId) {
        ChatRoom room = getChatRoomById(roomId);
        room.updateActivity();
        chatRepository.saveRoom(room);
    }

    private void deleteRoom(String roomId) {
        if (chatRepository instanceof RedisChatRepository) {
            ((RedisChatRepository) chatRepository).deleteRoom(roomId);
        }
    }
}