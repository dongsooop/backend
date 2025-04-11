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
        checkUserAccess(room, userId);
    }

    private void checkUserAccess(ChatRoom room, String userId) {
        if (ANONYMOUS_USER.equals(userId)) {
            return;
        }

        if (room.isKicked(userId)) {
            throw new UserKickedException(room.getRoomId());
        }

        if (!room.getParticipants().contains(userId)) {
            room.getParticipants().add(userId);
            chatRepository.saveRoom(room);
        }
    }

    public void validateSelfChat(String user1, String user2) {
        if (user1.equals(user2)) {
            throw new SelfChatException();
        }
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

    private void validateRequiredMessageFields(ChatMessage message) {
        if (message == null) {
            throw new InvalidChatRequestException();
        }

        if (!hasRequiredFields(message)) {
            throw new InvalidChatRequestException();
        }
    }

    private boolean hasRequiredFields(ChatMessage message) {
        return StringUtils.hasText(message.getRoomId()) &&
                StringUtils.hasText(message.getSenderId());
    }

    private ChatMessage enrichMessage(ChatMessage message) {
        if (message.getMessageId() == null) {
            String newMessageId = UUID.randomUUID().toString();
            message.setMessageId(newMessageId);
        }

        if (message.getTimestamp() == null) {
            LocalDateTime now = LocalDateTime.now();
            message.setTimestamp(now);
        }

        if (message.getType() == null) {
            message.setType(MessageType.CHAT);
        }

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

    public void validateManagerPermission(ChatRoom room, String userId) {
        String managerId = room.getManagerId();
        if (managerId != null && !managerId.equals(userId)) {
            throw new UnauthorizedManagerActionException();
        }
    }

    public void validateKickableUser(ChatRoom room, String userToKick) {
        if (!room.getParticipants().contains(userToKick)) {
            throw new UserNotInRoomException();
        }

        String managerId = room.getManagerId();
        if (managerId != null && managerId.equals(userToKick)) {
            throw new ManagerKickAttemptException();
        }
    }
}