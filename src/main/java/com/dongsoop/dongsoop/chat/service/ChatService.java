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

@Slf4j
@Service
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatValidator chatValidator;
    private final ChatSyncService chatSyncService;

    public ChatService(@Qualifier("redisChatRepository") ChatRepository chatRepository,
                       ChatValidator chatValidator,
                       ChatSyncService chatSyncService) {
        this.chatRepository = chatRepository;
        this.chatValidator = chatValidator;
        this.chatSyncService = chatSyncService;
    }

    public ChatRoom createOneToOneChatRoom(String userId, String targetUserId) {
        chatValidator.validateSelfChat(userId, targetUserId);

        if ("anonymousUser".equals(userId) || "anonymousUser".equals(targetUserId)) {
            throw new UnauthorizedChatAccessException();
        }

        return chatRepository.findRoomByParticipants(userId, targetUserId)
                .orElseGet(() -> createRoom(userId, targetUserId));
    }

    public ChatRoom createGroupChatRoom(String creatorId, Set<String> participants) {
        validateGroupParticipants(participants);
        participants.add(creatorId);

        ChatRoom room = ChatRoom.createWithParticipants(participants, creatorId);
        return chatRepository.saveRoom(room);
    }

    public ChatRoom kickUserFromRoom(String roomId, String requesterId, String userToKick) {
        ChatRoom room = getChatRoomById(roomId);

        chatValidator.validateManagerPermission(room, requesterId);
        chatValidator.validateKickableUser(room, userToKick);

        room.kickUser(userToKick);
        chatRepository.saveRoom(room);

        createUserKickedMessage(roomId, requesterId, userToKick);

        return room;
    }

    private ChatMessage createUserKickedMessage(String roomId, String managerId, String kickedUserId) {
        String content = kickedUserId + "님이 채팅방에서 추방되었습니다.";
        ChatMessage message = createSystemMessage(roomId, managerId, content, MessageType.LEAVE);
        chatRepository.saveMessage(message);
        return message;
    }

    public void enterChatRoom(String roomId, String userId) {
        chatValidator.validateUserForRoom(roomId, userId);
    }

    public ChatMessage processMessage(ChatMessage message) {
        ChatMessage validatedMessage = chatValidator.validateAndEnrichMessage(message);
        chatRepository.saveMessage(validatedMessage);
        return validatedMessage;
    }

    public ChatMessage createEnterMessage(String roomId, String userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        ChatMessage message = createSystemMessage(
                roomId,
                userId,
                userId + "님이 입장하셨습니다.",
                MessageType.ENTER
        );

        chatRepository.saveMessage(message);
        return message;
    }

    public List<ChatMessage> getChatHistory(String roomId, String userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        List<ChatMessage> messages = chatRepository.findMessagesByRoomId(roomId);

        if (messages.isEmpty()) {
            messages = chatSyncService.restoreMessagesFromDatabase(roomId);
        }

        return messages;
    }

    public ChatRoom getChatRoomById(String roomId) {
        return chatRepository.findRoomById(roomId)
                .orElseGet(() -> Optional.ofNullable(chatSyncService.restoreGroupChatRoom(roomId))
                        .orElseThrow(ChatRoomNotFoundException::new));
    }

    public List<ChatMessage> syncMessages(String roomId, String userId, List<ChatMessage> clientMessages) {
        chatValidator.validateUserForRoom(roomId, userId);
        List<ChatMessage> serverMessages = chatRepository.findMessagesByRoomId(roomId);
        List<ChatMessage> newMessages = chatValidator.filterDuplicateMessages(serverMessages, clientMessages);

        newMessages.forEach(this::processMessage);

        return chatRepository.findMessagesByRoomId(roomId);
    }

    public void recreateRoomIfNeeded(String roomId, String userId, List<ChatMessage> localMessages) {
        try {
            validateAndUpdateRoom(roomId, userId);
            syncMessages(roomId, userId, localMessages);
        } catch (ChatRoomNotFoundException e) {
            handleRoomNotFound(roomId, userId, localMessages);
        }
    }

    private void validateAndUpdateRoom(String roomId, String userId) {
        ChatRoom room = getChatRoomById(roomId);

        if (!room.getParticipants().contains(userId)) {
            room.getParticipants().add(userId);
            chatRepository.saveRoom(room);
        }
    }

    private void handleRoomNotFound(String roomId, String userId, List<ChatMessage> localMessages) {
        if (!localMessages.isEmpty()) {
            recreateRoomFromMessages(roomId, userId, localMessages);
        } else {
            throw new ChatRoomNotFoundException();
        }
    }

    public ChatRoom recreateRoomFromMessages(String roomId, String userId, List<ChatMessage> clientMessages) {
        if (clientMessages.isEmpty()) {
            throw new IllegalArgumentException("메시지가 없어 채팅방을 재생성할 수 없습니다.");
        }

        Set<String> participants = extractParticipantsFromMessages(userId, clientMessages);
        ChatRoom newRoom = createRoomWithId(roomId, participants);

        clientMessages.forEach(message -> {
            enrichMessageIfNeeded(message);
            chatRepository.saveMessage(message);
        });

        return newRoom;
    }

    private void enrichMessageIfNeeded(ChatMessage message) {
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
        if (message.getType() == null) {
            message.setType(MessageType.CHAT);
        }
    }

    private Set<String> extractParticipantsFromMessages(String userId, List<ChatMessage> messages) {
        Set<String> participants = new HashSet<>();
        participants.add(userId);

        messages.forEach(message -> participants.add(message.getSenderId()));

        return participants;
    }

    private ChatRoom createRoomWithId(String roomId, Set<String> participants) {
        ChatRoom newRoom = ChatRoom.builder()
                .roomId(roomId)
                .participants(participants)
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();

        return chatRepository.saveRoom(newRoom);
    }

    private ChatMessage createSystemMessage(String roomId, String userId, String content, MessageType type) {
        return ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(userId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .type(type)
                .build();
    }

    private ChatRoom createRoom(String user1, String user2) {
        ChatRoom room = ChatRoom.create(user1, user2);
        return chatRepository.saveRoom(room);
    }

    private void validateGroupParticipants(Set<String> participants) {
        if (participants.size() < 2) {
            throw new IllegalArgumentException("그룹 채팅에는 최소 2명 이상의 참여자가 필요합니다.");
        }
    }

    public List<ChatRoom> getRoomsForUserId(String userId) {
        List<ChatRoom> rooms = chatRepository.findRoomsByUserId(userId);
        return rooms.stream()
                .filter(room -> !room.isKicked(userId))
                .toList();
    }
}