package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.exception.domain.websocket.ChatRoomNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final ChatValidator chatValidator;

    // 1:1 채팅방 생성
    public ChatRoom createOneToOneChatRoom(String userId, String targetUserId) {
        log.info("Creating 1:1 chat room between users: {} and {}", userId, targetUserId);
        chatValidator.validateSelfChat(userId, targetUserId);
        return chatRepository.findRoomByParticipants(userId, targetUserId)
                .orElseGet(() -> createRoom(userId, targetUserId));
    }

    // 채팅방 입장
    public void enterChatRoom(String roomId, String userId) {
        ChatRoom room = getChatRoomById(roomId);
        updateRoomParticipants(room, userId);
    }

    // @SendTo 사용을 위한 메시지 처리 메소드
    public ChatMessage processMessage(ChatMessage message) {
        chatValidator.validateUserForRoom(message.getRoomId(), message.getSenderId());
        prepareMessage(message);
        chatRepository.saveMessage(message);
        return message;
    }

    // 입장 메시지 생성
    public ChatMessage createEnterMessage(String roomId, String userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        ChatRoom room = getChatRoomById(roomId);
        updateRoomParticipants(room, userId);

        ChatMessage systemMessage = createSystemMessage(
                roomId, userId, userId + "님이 입장했습니다.", MessageType.ENTER);
        chatRepository.saveMessage(systemMessage);
        return systemMessage;
    }

    // 채팅 내역 조회
    public List<ChatMessage> getChatHistory(String roomId, String userId) {
        chatValidator.validateUserForRoom(roomId, userId);
        return chatRepository.findMessagesByRoomId(roomId);
    }

    // 메시지 동기화
    public List<ChatMessage> syncMessages(String roomId, String userId, List<ChatMessage> clientMessages) {
        chatValidator.validateUserForRoom(roomId, userId);

        if (isEmptyClientMessages(clientMessages)) {
            return chatRepository.findMessagesByRoomId(roomId);
        }

        List<ChatMessage> serverMessages = chatRepository.findMessagesByRoomId(roomId);
        List<ChatMessage> preparedMessages = prepareClientMessages(clientMessages);
        List<ChatMessage> newMessages = getNewMessages(serverMessages, preparedMessages);

        saveMessages(newMessages);

        return chatRepository.findMessagesByRoomId(roomId);
    }

    // 헬퍼 메소드들은 그대로 유지
    private ChatRoom createRoom(String userId, String targetUserId) {
        return chatRepository.saveRoom(ChatRoom.create(userId, targetUserId));
    }

    private ChatRoom getChatRoomById(String roomId) {
        return chatRepository.findRoomById(roomId)
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    private void updateRoomParticipants(ChatRoom room, String userId) {
        boolean userAdded = room.getParticipants().add(userId);
        if (userAdded) {
            chatRepository.saveRoom(room);
        }
    }

    private ChatMessage createSystemMessage(String roomId, String senderId,
                                            String content, MessageType type) {
        return ChatMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .roomId(roomId)
                .senderId(senderId)
                .content(content)
                .timestamp(LocalDateTime.now())
                .type(type)
                .build();
    }

    private void prepareMessage(ChatMessage message) {
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

    private boolean isEmptyClientMessages(List<ChatMessage> messages) {
        return messages == null || messages.isEmpty();
    }

    private List<ChatMessage> prepareClientMessages(List<ChatMessage> messages) {
        return messages.stream()
                .map(chatValidator::validateAndEnrichMessage)
                .toList();
    }

    private List<ChatMessage> getNewMessages(List<ChatMessage> serverMessages,
                                             List<ChatMessage> clientMessages) {
        return chatValidator.filterDuplicateMessages(serverMessages, clientMessages);
    }

    private void saveMessages(List<ChatMessage> messages) {
        messages.forEach(chatRepository::saveMessage);
    }
}