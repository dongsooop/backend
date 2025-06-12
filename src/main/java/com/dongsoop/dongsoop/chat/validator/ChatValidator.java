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
import java.util.Objects;
import java.util.UUID;

@Component
public class ChatValidator {
    private static final Long ANONYMOUS_USER_ID = -1L;

    private final ChatRepository chatRepository;
    private final ChatSyncService chatSyncService;

    public ChatValidator(@Qualifier("redisChatRepository") ChatRepository chatRepository,
                         ChatSyncService chatSyncService) {
        this.chatRepository = chatRepository;
        this.chatSyncService = chatSyncService;
    }

    public void validateUserForRoom(String roomId, Long userId) {
        ChatRoom room = chatSyncService.findRoomOrRestore(roomId);

        if (room.isKicked(userId)) {
            throw new UserKickedException(roomId);
        }

        addUserToRoomIfNeeded(room, userId);
    }

    public void validateSelfChat(Long user1, Long user2) {
        if (user1.equals(user2)) {
            throw new SelfChatException();
        }
    }

    public ChatMessage validateAndEnrichMessage(ChatMessage message) {
        validateMessageRequirements(message);
        validateUserForRoom(message.getRoomId(), message.getSenderId());

        enrichMessage(message);
        return message;
    }

    public void validateManagerPermission(ChatRoom room, Long requesterId) {
        if (!room.isGroupChat()) {
            throw new IllegalArgumentException("1:1 채팅방에서는 강퇴할 수 없습니다.");
        }

        Long managerId = room.getManagerId();
        if (managerId == null || !Objects.equals(managerId, requesterId)) {
            throw new UnauthorizedManagerActionException();
        }
    }

    public void validateKickableUser(ChatRoom room, Long userToKick) {
        if (!room.getParticipants().contains(userToKick)) {
            throw new UserNotInRoomException();
        }

        if (Objects.equals(room.getManagerId(), userToKick)) {
            throw new ManagerKickAttemptException();
        }
    }

    private void addUserToRoomIfNeeded(ChatRoom room, Long userId) {
        boolean isAnonymous = ANONYMOUS_USER_ID.equals(userId);
        boolean isAlreadyParticipant = room.getParticipants().contains(userId);

        if (!isAnonymous && !isAlreadyParticipant) {
            room.addNewParticipant(userId);
            chatRepository.saveRoom(room);
        }
    }

    private void validateMessageRequirements(ChatMessage message) {
        boolean hasRoomId = StringUtils.hasText(message.getRoomId());
        boolean hasSenderId = message.getSenderId() != null;

        if (!hasRoomId || !hasSenderId) {
            throw new InvalidChatRequestException();
        }
    }

    private void enrichMessage(ChatMessage message) {
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
}