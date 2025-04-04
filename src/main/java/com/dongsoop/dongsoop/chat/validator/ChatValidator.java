package com.dongsoop.dongsoop.chat.validator;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatSyncService;
import com.dongsoop.dongsoop.exception.domain.websocket.*;
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

    public void validateUserForRoom(String roomId, String userId) {
        ChatRoom room = getRoomOrRestore(roomId);
        addUserToRoomIfNeeded(room, userId);
        ensureUserInRoom(room, userId);
    }

    public void validateSelfChat(String user1, String user2) {
        validate(() -> user1.equals(user2), SelfChatException::new);
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

    private ChatRoom getRoomOrRestore(String roomId) {
        return chatRepository.findRoomById(roomId)
                .orElseGet(() -> Optional.ofNullable(chatSyncService.restoreGroupChatRoom(roomId))
                        .orElseThrow(ChatRoomNotFoundException::new));
    }

    private void addUserToRoomIfNeeded(ChatRoom room, String userId) {
        if (room.isKicked(userId)) {
            throw new UserKickedException(room.getRoomId());
        }

        if (!room.getParticipants().contains(userId)) {
            room.getParticipants().add(userId);
            chatRepository.saveRoom(room);
        }
    }

    private void ensureUserInRoom(ChatRoom room, String userId) {
        validate(() -> !room.getParticipants().contains(userId),
                UnauthorizedChatAccessException::new);
    }

    private void validateRequiredMessageFields(ChatMessage message) {
        validate(() -> message == null, InvalidChatRequestException::new);
        validate(() -> !hasRequiredFields(message), InvalidChatRequestException::new);
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
                .filter(Boolean::booleanValue)
                .ifPresent(result -> {
                    throw exceptionSupplier.get();
                });
    }

    public void validateManagerPermission(ChatRoom room, String userId) {
        Optional.ofNullable(room.getManagerId())
                .filter(managerId -> !managerId.equals(userId))
                .ifPresent(managerId -> {
                    throw new UnauthorizedManagerActionException();
                });
    }

    public void validateKickableUser(ChatRoom room, String userToKick) {
        Optional.of(room.getParticipants())
                .filter(participants -> !participants.contains(userToKick))
                .ifPresent(p -> {
                    throw new UserNotInRoomException();
                });

        Optional.ofNullable(room.getManagerId())
                .filter(id -> id.equals(userToKick))
                .ifPresent(id -> {
                    throw new ManagerKickAttemptException();
                });
    }
}