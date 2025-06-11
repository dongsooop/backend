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
                         ChatSyncService chatSyncService
    ) {
        this.chatRepository = chatRepository;
        this.chatSyncService = chatSyncService;
    }

    public void validateUserForRoom(String roomId, Long userId) {
        ChatRoom room = chatSyncService.findRoomOrRestore(roomId);

        validateUserNotKicked(room, userId);
        addUserToRoomIfNeeded(room, userId);
    }

    public void validateSelfChat(Long user1, Long user2) {
        validateNotSelfChat(user1, user2);
    }

    public ChatMessage validateAndEnrichMessage(ChatMessage message) {
        validateMessageRequirements(message);
        validateUserForRoom(message.getRoomId(), message.getSenderId());

        enrichMessageId(message);
        enrichTimestamp(message);
        enrichMessageType(message);

        return message;
    }

    public void validateManagerPermission(ChatRoom room, Long requesterId) {
        validateIsGroupChat(room);
        validateManagerAuthority(room, requesterId);
    }

    public void validateKickableUser(ChatRoom room, Long userToKick) {
        validateUserExistsInRoom(room, userToKick);
        validateNotKickingManager(room, userToKick);
    }

    private void validateUserNotKicked(ChatRoom room, Long userId) {
        boolean userIsKicked = room.isKicked(userId);
        throwExceptionIfUserKicked(userIsKicked, room.getRoomId());
    }

    private void throwExceptionIfUserKicked(boolean userIsKicked, String roomId) {
        if (userIsKicked) {
            throw new UserKickedException(roomId);
        }
    }

    private void addUserToRoomIfNeeded(ChatRoom room, Long userId) {
        boolean shouldAddUser = shouldAddUserToRoom(userId, room);
        addUserToRoomIfShould(shouldAddUser, room, userId);
    }

    private boolean shouldAddUserToRoom(Long userId, ChatRoom room) {
        boolean isNotAnonymous = !ANONYMOUS_USER_ID.equals(userId);
        boolean isNotParticipant = !room.getParticipants().contains(userId);
        return isNotAnonymous && isNotParticipant;
    }

    private void addUserToRoomIfShould(boolean shouldAddUser, ChatRoom room, Long userId) {
        if (shouldAddUser) {
            room.getParticipants().add(userId);
            chatRepository.saveRoom(room);
        }
    }

    private void validateNotSelfChat(Long user1, Long user2) {
        boolean isSelfChat = user1.equals(user2);
        throwExceptionIfSelfChat(isSelfChat);
    }

    private void throwExceptionIfSelfChat(boolean isSelfChat) {
        if (isSelfChat) {
            throw new SelfChatException();
        }
    }

    private void validateMessageRequirements(ChatMessage message) {
        boolean hasRequiredFields = hasRequiredFields(message);
        throwExceptionIfInvalidMessage(hasRequiredFields);
    }

    private boolean hasRequiredFields(ChatMessage message) {
        boolean hasRoomId = StringUtils.hasText(message.getRoomId());
        boolean hasSenderId = message.getSenderId() != null;
        return hasRoomId && hasSenderId;
    }

    private void throwExceptionIfInvalidMessage(boolean hasRequiredFields) {
        boolean isInvalid = !hasRequiredFields;
        if (isInvalid) {
            throw new InvalidChatRequestException();
        }
    }

    private void enrichMessageId(ChatMessage message) {
        boolean messageIdIsNull = message.getMessageId() == null;
        setMessageIdIfNull(messageIdIsNull, message);
    }

    private void setMessageIdIfNull(boolean messageIdIsNull, ChatMessage message) {
        if (messageIdIsNull) {
            message.setMessageId(UUID.randomUUID().toString());
        }
    }

    private void enrichTimestamp(ChatMessage message) {
        boolean timestampIsNull = message.getTimestamp() == null;
        setTimestampIfNull(timestampIsNull, message);
    }

    private void setTimestampIfNull(boolean timestampIsNull, ChatMessage message) {
        if (timestampIsNull) {
            message.setTimestamp(LocalDateTime.now());
        }
    }

    private void enrichMessageType(ChatMessage message) {
        boolean typeIsNull = message.getType() == null;
        setTypeIfNull(typeIsNull, message);
    }

    private void setTypeIfNull(boolean typeIsNull, ChatMessage message) {
        if (typeIsNull) {
            message.setType(MessageType.CHAT);
        }
    }

    private void validateIsGroupChat(ChatRoom room) {
        boolean isNotGroupChat = !room.isGroupChat();
        throwExceptionIfNotGroupChat(isNotGroupChat);
    }

    private void throwExceptionIfNotGroupChat(boolean isNotGroupChat) {
        if (isNotGroupChat) {
            throw new IllegalArgumentException("1:1 채팅방에서는 강퇴할 수 없습니다.");
        }
    }

    private void validateManagerAuthority(ChatRoom room, Long requesterId) {
        Long managerId = room.getManagerId();
        boolean hasNoManager = managerId == null;
        boolean isNotManager = !Objects.requireNonNull(managerId).equals(requesterId);
        throwExceptionIfUnauthorizedManager(hasNoManager, isNotManager);
    }

    private void throwExceptionIfUnauthorizedManager(boolean hasNoManager, boolean isNotManager) {
        boolean isUnauthorized = hasNoManager || isNotManager;
        if (isUnauthorized) {
            throw new UnauthorizedManagerActionException();
        }
    }

    private void validateUserExistsInRoom(ChatRoom room, Long userToKick) {
        boolean userNotInRoom = !room.getParticipants().contains(userToKick);
        throwExceptionIfUserNotInRoom(userNotInRoom);
    }

    private void throwExceptionIfUserNotInRoom(boolean userNotInRoom) {
        if (userNotInRoom) {
            throw new UserNotInRoomException();
        }
    }

    private void validateNotKickingManager(ChatRoom room, Long userToKick) {
        Long managerId = room.getManagerId();
        boolean isKickingManager = Objects.equals(managerId, userToKick);

        if (isKickingManager) {
            throw new ManagerKickAttemptException();
        }
    }
}