package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.exception.domain.websocket.ChatRoomNotFoundException;
import com.dongsoop.dongsoop.exception.domain.websocket.UnauthorizedChatAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
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
        validateNegativeUserIds(userId, targetUserId);

        return findExistingRoomOrCreate(userId, targetUserId);
    }

    public ChatRoom createGroupChatRoom(Long creatorId, Set<Long> participants, String title) {
        validateMinimumParticipants(participants);
        ChatRoom room = ChatRoom.createWithParticipantsAndTitle(participants, creatorId, title);
        return chatRepository.saveRoom(room);
    }

    public ChatRoom kickUserFromRoom(String roomId, Long requesterId, Long userToKickId) {
        ChatRoom room = getChatRoomById(roomId);

        chatValidator.validateManagerPermission(room, requesterId);
        chatValidator.validateKickableUser(room, userToKickId);
        executeUserKick(room, userToKickId);
        createKickNotification(roomId, userToKickId);

        return chatRepository.saveRoom(room);
    }

    public void enterChatRoom(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
    }

    public ChatMessage processMessage(ChatMessage message) {
        ChatMessage enrichedMessage = chatValidator.validateAndEnrichMessage(message);
        chatRepository.saveMessage(enrichedMessage);
        return enrichedMessage;
    }

    public ChatMessage createEnterMessage(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        return buildAndSaveEnterMessage(roomId, userId);
    }

    public List<ChatMessage> getChatHistory(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        return retrieveMessagesFromCacheOrDatabase(roomId);
    }

    public ChatRoom getChatRoomById(String roomId) {
        return findRoomByIdOrRestore(roomId);
    }

    public List<ChatMessage> syncMessages(String roomId, Long userId, List<ChatMessage> clientMessages) {
        chatValidator.validateUserForRoom(roomId, userId);

        List<ChatMessage> serverMessages = chatRepository.findMessagesByRoomId(roomId);
        List<ChatMessage> newMessages = chatValidator.filterDuplicateMessages(serverMessages, clientMessages);

        processNewMessages(newMessages);
        return chatRepository.findMessagesByRoomId(roomId);
    }

    public void recreateRoomIfNeeded(String roomId, Long userId, List<ChatMessage> localMessages) {
        attemptRoomRecreation(roomId, userId, localMessages);
    }

    public List<ChatRoom> getRoomsForUserId(Long userId) {
        List<ChatRoom> allRooms = chatRepository.findRoomsByUserId(userId);
        return filterNonKickedRooms(allRooms, userId);
    }

    private void validateNegativeUserIds(Long userId, Long targetUserId) {
        validatePositiveUserId(userId);
        validatePositiveUserId(targetUserId);
    }

    private void validatePositiveUserId(Long userId) {
        if (userId < 0) {
            throw new UnauthorizedChatAccessException();
        }
    }

    private ChatRoom findExistingRoomOrCreate(Long userId, Long targetUserId) {
        ChatRoom existingRoom = chatRepository.findRoomByParticipants(userId, targetUserId).orElse(null);
        return getExistingOrNewRoom(existingRoom, userId, targetUserId);
    }

    private ChatRoom getExistingOrNewRoom(ChatRoom existingRoom, Long userId, Long targetUserId) {
        if (existingRoom != null) {
            return existingRoom;
        }
        return createNewOneToOneRoom(userId, targetUserId);
    }

    private ChatRoom createNewOneToOneRoom(Long user1, Long user2) {
        ChatRoom room = ChatRoom.create(user1, user2);
        return chatRepository.saveRoom(room);
    }

    private void validateMinimumParticipants(Set<Long> participants) {
        if (participants.size() < 1) {
            throw new IllegalArgumentException("그룹 채팅 참여자 수가 올바르지 않습니다.");
        }
    }

    private void executeUserKick(ChatRoom room, Long userToKickId) {
        room.kickUser(userToKickId);
    }

    private void createKickNotification(String roomId, Long kickedUserId) {
        String content = kickedUserId.toString();
        ChatMessage notification = buildSystemMessage(roomId, kickedUserId, content, MessageType.LEAVE);
        chatRepository.saveMessage(notification);
    }

    private ChatMessage buildAndSaveEnterMessage(String roomId, Long userId) {
        String content = userId.toString();
        ChatMessage enterMessage = buildSystemMessage(roomId, userId, content, MessageType.ENTER);
        chatRepository.saveMessage(enterMessage);
        return enterMessage;
    }

    private ChatMessage buildSystemMessage(String roomId, Long senderId, String content, MessageType type) {
        return ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(senderId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .type(type)
                .build();
    }

    private List<ChatMessage> retrieveMessagesFromCacheOrDatabase(String roomId) {
        List<ChatMessage> cachedMessages = chatRepository.findMessagesByRoomId(roomId);
        return getCachedOrRestoredMessages(cachedMessages, roomId);
    }

    private List<ChatMessage> getCachedOrRestoredMessages(List<ChatMessage> cachedMessages, String roomId) {
        if (!cachedMessages.isEmpty()) {
            return cachedMessages;
        }
        return chatSyncService.restoreMessagesFromDatabase(roomId);
    }

    private ChatRoom findRoomByIdOrRestore(String roomId) {
        ChatRoom room = chatRepository.findRoomById(roomId).orElse(null);
        return getRoomOrRestore(room, roomId);
    }

    private ChatRoom getRoomOrRestore(ChatRoom room, String roomId) {
        if (room != null) {
            return room;
        }
        return restoreRoomFromDatabase(roomId);
    }

    private ChatRoom restoreRoomFromDatabase(String roomId) {
        ChatRoom restoredRoom = chatSyncService.restoreGroupChatRoom(roomId);
        if (restoredRoom == null) {
            throw new ChatRoomNotFoundException();
        }
        return restoredRoom;
    }

    private void processNewMessages(List<ChatMessage> newMessages) {
        newMessages.forEach(this::processMessage);
    }

    private void attemptRoomRecreation(String roomId, Long userId, List<ChatMessage> localMessages) {
        boolean roomRecreated = tryUpdateExistingRoom(roomId, userId, localMessages);
        if (!roomRecreated) {
            recreateFromMessages(roomId, userId, localMessages);
        }
    }

    private boolean tryUpdateExistingRoom(String roomId, Long userId, List<ChatMessage> localMessages) {
        try {
            updateExistingRoomParticipants(roomId, userId);
            syncMessages(roomId, userId, localMessages);
            return true;
        } catch (ChatRoomNotFoundException e) {
            return false;
        }
    }

    private void updateExistingRoomParticipants(String roomId, Long userId) {
        ChatRoom room = getChatRoomById(roomId);
        boolean shouldAddUser = !room.getParticipants().contains(userId);
        if (shouldAddUser) {
            room.getParticipants().add(userId);
            chatRepository.saveRoom(room);
        }
    }

    private void recreateFromMessages(String roomId, Long userId, List<ChatMessage> clientMessages) {
        validateNonEmptyMessages(clientMessages);

        Set<Long> participants = extractParticipantsFromMessages(userId, clientMessages);
        createRoomWithSpecificId(roomId, participants);
        saveEnrichedMessages(clientMessages);
    }

    private void validateNonEmptyMessages(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            throw new IllegalArgumentException("메시지가 없어 채팅방을 재생성할 수 없습니다.");
        }
    }

    private Set<Long> extractParticipantsFromMessages(Long userId, List<ChatMessage> messages) {
        Set<Long> participants = new HashSet<>();
        participants.add(userId);

        messages.stream()
                .map(ChatMessage::getSenderId)
                .forEach(participants::add);

        return participants;
    }

    private void createRoomWithSpecificId(String roomId, Set<Long> participants) {
        boolean isGroupChat = participants.size() > 2;
        Long managerId = null;

        if (isGroupChat) {
            managerId = participants.iterator().next();
        }

        ChatRoom newRoom = ChatRoom.builder()
                .roomId(roomId)
                .participants(participants)
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .isGroupChat(isGroupChat)
                .managerId(managerId)
                .build();

        chatRepository.saveRoom(newRoom);
    }

    private void saveEnrichedMessages(List<ChatMessage> clientMessages) {
        clientMessages.stream()
                .peek(this::enrichMessageFields)
                .forEach(chatRepository::saveMessage);
    }

    private void enrichMessageFields(ChatMessage message) {
        setMessageIdIfMissing(message);
        setTimestampIfMissing(message);
        setTypeIfMissing(message);
    }

    private void setMessageIdIfMissing(ChatMessage message) {
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }
    }

    private void setTimestampIfMissing(ChatMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
    }

    private void setTypeIfMissing(ChatMessage message) {
        if (message.getType() == null) {
            message.setType(MessageType.CHAT);
        }
    }

    private List<ChatRoom> filterNonKickedRooms(List<ChatRoom> rooms, Long userId) {
        return rooms.stream()
                .filter(room -> !room.isKicked(userId))
                .toList();
    }
}