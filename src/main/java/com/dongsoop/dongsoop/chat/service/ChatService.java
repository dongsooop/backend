package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.exception.domain.websocket.ChatRoomNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatValidator chatValidator;

    public ChatRoom createOneToOneChatRoom(String userId, String targetUserId) {
        chatValidator.validateSelfChat(userId, targetUserId);
        return chatRepository.findRoomByParticipants(userId, targetUserId)
                .orElseGet(() -> createRoom(userId, targetUserId));
    }

    public void enterChatRoom(String roomId, String userId) {
        updateRoomParticipants(getChatRoomById(roomId), userId);
    }

    public ChatMessage processMessage(ChatMessage message) {
        try {
            chatValidator.validateUserForRoom(message.getRoomId(), message.getSenderId());
        } catch (ChatRoomNotFoundException e) {
            autoRecreateRoom(message.getRoomId(), message.getSenderId());
            chatValidator.validateUserForRoom(message.getRoomId(), message.getSenderId());
        }

        ChatMessage enrichedMessage = chatValidator.validateAndEnrichMessage(message);
        chatRepository.saveMessage(enrichedMessage);
        return enrichedMessage;
    }

    public ChatMessage createEnterMessage(String roomId, String userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        updateRoomParticipants(getChatRoomById(roomId), userId);

        ChatMessage systemMessage = createSystemMessage(
                roomId, userId, userId + "님이 입장했습니다.", MessageType.ENTER);
        chatRepository.saveMessage(systemMessage);
        return systemMessage;
    }

    public List<ChatMessage> getChatHistory(String roomId, String userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        return chatRepository.findMessagesByRoomId(roomId);
    }

    public List<ChatMessage> syncMessages(String roomId, String userId, List<ChatMessage> clientMessages) {
        chatValidator.validateUserForRoom(roomId, userId);

        if (isEmptyCollection(clientMessages)) {
            return chatRepository.findMessagesByRoomId(roomId);
        }

        List<ChatMessage> serverMessages = chatRepository.findMessagesByRoomId(roomId);
        List<ChatMessage> preparedMessages = prepareClientMessages(clientMessages);
        List<ChatMessage> newMessages = chatValidator.filterDuplicateMessages(serverMessages, preparedMessages);

        saveMessages(newMessages);
        return chatRepository.findMessagesByRoomId(roomId);
    }

    public ChatRoom getChatRoomById(String roomId) {
        return chatRepository.findRoomById(roomId)
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    public void recreateRoomIfNeeded(String roomId, String userId, List<ChatMessage> clientMessages) {
        try {
            getChatRoomById(roomId);
        } catch (ChatRoomNotFoundException e) {
            recreateRoomFromMessages(roomId, userId, clientMessages);
        }
    }

    private void autoRecreateRoom(String roomId, String userId) {
        Set<String> participants = new HashSet<>();
        participants.add(userId);

        ChatRoom newRoom = ChatRoom.builder()
                .roomId(roomId)
                .participants(participants)
                .build();

        chatRepository.saveRoom(newRoom);
    }

    private <T> boolean isEmptyCollection(List<T> collection) {
        return collection == null || collection.isEmpty();
    }

    private ChatRoom createRoom(String userId, String targetUserId) {
        return chatRepository.saveRoom(ChatRoom.create(userId, targetUserId));
    }

    private void updateRoomParticipants(ChatRoom room, String userId) {
        boolean userAdded = room.getParticipants().add(userId);
        if (userAdded) {
            chatRepository.saveRoom(room);
        }
    }

    private ChatMessage createSystemMessage(String roomId, String senderId, String content, MessageType type) {
        return ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(senderId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .type(type)
                .build();
    }

    private List<ChatMessage> prepareClientMessages(List<ChatMessage> messages) {
        return messages.stream()
                .map(chatValidator::validateAndEnrichMessage)
                .toList();
    }

    private void saveMessages(List<ChatMessage> messages) {
        messages.forEach(chatRepository::saveMessage);
    }

    private void recreateRoomFromMessages(String roomId, String userId, List<ChatMessage> clientMessages) {
        Set<String> participants = extractParticipantsFromMessages(userId, clientMessages);

        ChatRoom newRoom = ChatRoom.builder()
                .roomId(roomId)
                .participants(participants)
                .build();

        chatRepository.saveRoom(newRoom);
        clientMessages.forEach(chatRepository::saveMessage);
    }

    private Set<String> extractParticipantsFromMessages(String userId, List<ChatMessage> messages) {
        Set<String> participants = new HashSet<>();
        participants.add(userId);

        messages.stream()
                .map(ChatMessage::getSenderId)
                .forEach(participants::add);

        return participants;
    }
}