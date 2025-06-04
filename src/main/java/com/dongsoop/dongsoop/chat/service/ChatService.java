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
import java.util.*;
import java.util.stream.Stream;

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

        Set<Long> allParticipants = buildParticipantSet(participants, creatorId);
        ChatRoom room = ChatRoom.createWithParticipantsAndTitle(allParticipants, creatorId, title);
        return chatRepository.saveRoom(room);
    }

    public ChatRoom kickUserFromRoom(String roomId, Long requesterId, Long userToKick) {
        ChatRoom room = getChatRoomById(roomId);
        chatValidator.validateManagerPermission(room, requesterId);
        chatValidator.validateKickableUser(room, userToKick);

        executeUserKick(room, userToKick);
        createKickNotification(roomId, requesterId, userToKick);

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
        Stream.of(userId, targetUserId)
                .filter(id -> id < 0)
                .findAny()
                .ifPresent(id -> {
                    throw new UnauthorizedChatAccessException();
                });
    }

    private ChatRoom findExistingRoomOrCreate(Long userId, Long targetUserId) {
        return chatRepository.findRoomByParticipants(userId, targetUserId)
                .orElseGet(() -> createNewOneToOneRoom(userId, targetUserId));
    }

    private ChatRoom createNewOneToOneRoom(Long user1, Long user2) {
        ChatRoom room = ChatRoom.create(user1, user2);
        return chatRepository.saveRoom(room);
    }

    private void validateMinimumParticipants(Set<Long> participants) {
        Optional.of(participants.size())
                .filter(size -> size < 0)
                .ifPresent(size -> {
                    throw new IllegalArgumentException("그룹 채팅 참여자 수가 올바르지 않습니다.");
                });
    }

    private Set<Long> buildParticipantSet(Set<Long> participants, Long creatorId) {
        Set<Long> allParticipants = new HashSet<>(participants);
        allParticipants.add(creatorId);
        return allParticipants;
    }

    private void executeUserKick(ChatRoom room, Long userToKick) {
        room.kickUser(userToKick);
    }

    private void createKickNotification(String roomId, Long managerId, Long kickedUserId) {
        String content = "사용자가 채팅방에서 추방되었습니다.";

        ChatMessage notification = buildSystemMessage(roomId, managerId, "시스템", content, MessageType.LEAVE);
        chatRepository.saveMessage(notification);
    }

    private ChatMessage buildAndSaveEnterMessage(String roomId, Long userId) {
        String content = "사용자가 입장하셨습니다.";
        ChatMessage enterMessage = buildSystemMessage(roomId, userId, "시스템", content, MessageType.ENTER);
        chatRepository.saveMessage(enterMessage);
        return enterMessage;
    }

    private ChatMessage buildSystemMessage(String roomId, Long senderId, String senderNickName, String content, MessageType type) {
        return ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(senderId)
                .senderNickName(senderNickName)
                .content(content)
                .timestamp(LocalDateTime.now())
                .type(type)
                .build();
    }

    private List<ChatMessage> retrieveMessagesFromCacheOrDatabase(String roomId) {
        List<ChatMessage> cachedMessages = chatRepository.findMessagesByRoomId(roomId);

        return Optional.of(cachedMessages)
                .filter(messages -> !messages.isEmpty())
                .orElseGet(() -> chatSyncService.restoreMessagesFromDatabase(roomId));
    }

    private ChatRoom findRoomByIdOrRestore(String roomId) {
        return chatRepository.findRoomById(roomId)
                .orElseGet(() -> restoreRoomFromDatabase(roomId));
    }

    private ChatRoom restoreRoomFromDatabase(String roomId) {
        return Optional.ofNullable(chatSyncService.restoreGroupChatRoom(roomId))
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    private void processNewMessages(List<ChatMessage> newMessages) {
        newMessages.forEach(this::processMessage);
    }

    private void attemptRoomRecreation(String roomId, Long userId, List<ChatMessage> localMessages) {
        Optional.of(roomId)
                .map(id -> tryUpdateExistingRoom(id, userId, localMessages))
                .orElseGet(() -> recreateFromMessages(roomId, userId, localMessages));
    }

    private Boolean tryUpdateExistingRoom(String roomId, Long userId, List<ChatMessage> localMessages) {
        try {
            updateExistingRoomParticipants(roomId, userId);
            syncMessages(roomId, userId, localMessages);
            return true;
        } catch (ChatRoomNotFoundException e) {
            recreateFromMessages(roomId, userId, localMessages);
            return false;
        }
    }

    private void updateExistingRoomParticipants(String roomId, Long userId) {
        ChatRoom room = getChatRoomById(roomId);

        Optional.of(userId)
                .filter(id -> !room.getParticipants().contains(id))
                .ifPresent(id -> {
                    room.getParticipants().add(id);
                    chatRepository.saveRoom(room);
                });
    }

    private Boolean recreateFromMessages(String roomId, Long userId, List<ChatMessage> clientMessages) {
        validateNonEmptyMessages(clientMessages);

        Set<Long> participants = extractParticipantsFromMessages(userId, clientMessages);
        createRoomWithSpecificId(roomId, participants);
        saveEnrichedMessages(clientMessages);

        return true;
    }

    private void validateNonEmptyMessages(List<ChatMessage> messages) {
        Optional.of(messages)
                .filter(List::isEmpty)
                .ifPresent(emptyList -> {
                    throw new IllegalArgumentException("메시지가 없어 채팅방을 재생성할 수 없습니다.");
                });
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
        ChatRoom newRoom = ChatRoom.builder()
                .roomId(roomId)
                .participants(participants)
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
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
        setSenderNickNameIfMissing(message);
    }

    private void setMessageIdIfMissing(ChatMessage message) {
        Optional.ofNullable(message.getMessageId())
                .orElseGet(() -> {
                    String newId = UUID.randomUUID().toString();
                    message.setMessageId(newId);
                    return newId;
                });
    }

    private void setTimestampIfMissing(ChatMessage message) {
        Optional.ofNullable(message.getTimestamp())
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    message.setTimestamp(now);
                    return now;
                });
    }

    private void setTypeIfMissing(ChatMessage message) {
        Optional.ofNullable(message.getType())
                .orElseGet(() -> {
                    message.setType(MessageType.CHAT);
                    return MessageType.CHAT;
                });
    }

    private void setSenderNickNameIfMissing(ChatMessage message) {
        Optional.ofNullable(message.getSenderNickName())
                .orElseGet(() -> {
                    String defaultName = "사용자" + message.getSenderId();
                    message.setSenderNickName(defaultName);
                    return defaultName;
                });
    }

    private List<ChatRoom> filterNonKickedRooms(List<ChatRoom> rooms, Long userId) {
        return rooms.stream()
                .filter(room -> !room.isKicked(userId))
                .toList();
    }
}