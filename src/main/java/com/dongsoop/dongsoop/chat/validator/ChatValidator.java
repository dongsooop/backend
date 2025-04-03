package com.dongsoop.dongsoop.chat.validator;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatSyncService;
import com.dongsoop.dongsoop.exception.domain.websocket.ChatRoomNotFoundException;
import com.dongsoop.dongsoop.exception.domain.websocket.InvalidChatRequestException;
import com.dongsoop.dongsoop.exception.domain.websocket.SelfChatException;
import com.dongsoop.dongsoop.exception.domain.websocket.UnauthorizedChatAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ChatValidator {
    private static final String ANONYMOUS_USER = "anonymousUser";
    private final ChatRepository chatRepository;
    private final ChatSyncService chatSyncService;

    public ChatValidator(@Qualifier("redisChatRepository") ChatRepository chatRepository,
                         ChatSyncService chatSyncService) {
        this.chatRepository = chatRepository;
        this.chatSyncService = chatSyncService;
    }

    // ChatValidator.java 수정 필요
    public void validateUserForRoom(String roomId, String userId) {
        ChatRoom room = chatRepository.findRoomById(roomId)
                .orElseGet(() -> {
                    // Redis에 없는 경우 PostgreSQL에서 복원 시도
                    ChatRoom restoredRoom = chatSyncService.restoreGroupChatRoom(roomId);
                    if (restoredRoom != null) {
                        return restoredRoom;
                    }
                    throw new ChatRoomNotFoundException();
                });

        // 사용자가 참여자 목록에 없으면 자동으로 추가
        if (!room.getParticipants().contains(userId)) {
            log.info("사용자 {}가 채팅방 {} 참여자 목록에 없어 자동 추가합니다", userId, roomId);
            room.getParticipants().add(userId);
            chatRepository.saveRoom(room);
        }

        validate(() -> !room.getParticipants().contains(userId),
                UnauthorizedChatAccessException::new);
    }

    public void validateSelfChat(String user1, String user2) {
        validate(() -> user1.equals(user2),
                SelfChatException::new);
    }

    public ChatMessage validateAndEnrichMessage(ChatMessage message) {
        validateRequiredMessageFields(message);
        validateUserForRoom(message.getRoomId(), message.getSenderId());
        return enrichMessage(message);
    }

    public List<ChatMessage> filterDuplicateMessages(List<ChatMessage> serverMessages, List<ChatMessage> clientMessages) {
        Set<String> existingMessageIds = extractMessageIds(serverMessages);
        return filterMessages(clientMessages, msg -> !existingMessageIds.contains(msg.getMessageId()));
    }

    private ChatRoom findRoomOrThrow(String roomId) {
        return chatRepository.findRoomById(roomId)
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    private void validateUserAccess(ChatRoom room, String userId) {
        validateNotAnonymousUser(userId);
        addUserToRoomIfNeeded(room, userId);
        validateUserInRoom(room, userId);
    }

    private void validateNotAnonymousUser(String userId) {
        validate(() -> ANONYMOUS_USER.equals(userId),
                UnauthorizedChatAccessException::new);
    }

    private void validateUserInRoom(ChatRoom room, String userId) {
        validate(() -> !room.getParticipants().contains(userId),
                UnauthorizedChatAccessException::new);
    }

    private void validateRequiredMessageFields(ChatMessage message) {
        validate(() -> message == null,
                InvalidChatRequestException::new);

        validate(() -> !hasRequiredFields(message),
                InvalidChatRequestException::new);
    }

    private boolean hasRequiredFields(ChatMessage message) {
        return StringUtils.hasText(message.getRoomId()) &&
                StringUtils.hasText(message.getSenderId());
    }

    private ChatMessage enrichMessage(ChatMessage message) {
        message.setMessageId(Optional.ofNullable(message.getMessageId())
                .orElseGet(() -> UUID.randomUUID().toString()));

        message.setTimestamp(Optional.ofNullable(message.getTimestamp())
                .orElseGet(LocalDateTime::now));

        message.setType(Optional.ofNullable(message.getType())
                .orElse(MessageType.CHAT));

        return message;
    }

    private void addUserToRoomIfNeeded(ChatRoom room, String userId) {
        if (room.getParticipants().add(userId)) {
            chatRepository.saveRoom(room);
        }
    }

    private Set<String> extractMessageIds(List<ChatMessage> messages) {
        return messages.stream()
                .map(ChatMessage::getMessageId)
                .collect(Collectors.toSet());
    }

    private List<ChatMessage> filterMessages(List<ChatMessage> messages, Predicate<ChatMessage> filter) {
        return messages.stream()
                .filter(filter)
                .toList();
    }

    private <T extends RuntimeException> void validate(Supplier<Boolean> condition, Supplier<T> exceptionSupplier) {
        Optional.of(condition.get())
                .filter(result -> result)
                .ifPresent(result -> {
                    throw exceptionSupplier.get();
                });
    }
}
