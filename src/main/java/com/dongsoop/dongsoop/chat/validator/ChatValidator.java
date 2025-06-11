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
import java.util.Optional;
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
        validateUserNotKickedCondition(userIsKicked, room.getRoomId());
    }

    private void validateUserNotKickedCondition(boolean userIsKicked, String roomId) {
        Optional.of(userIsKicked)
                .filter(kicked -> kicked)
                .ifPresent(kicked -> {
                    throw new UserKickedException(roomId);
                });
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
        Optional.of(shouldAddUser)
                .filter(add -> add)
                .ifPresent(add -> {
                    room.getParticipants().add(userId);
                    chatRepository.saveRoom(room);
                });
    }

    private void validateNotSelfChat(Long user1, Long user2) {
        boolean isSelfChat = user1.equals(user2);
        validateSelfChatCondition(isSelfChat);
    }

    private void validateSelfChatCondition(boolean isSelfChat) {
        Optional.of(isSelfChat)
                .filter(selfChat -> selfChat)
                .ifPresent(selfChat -> {
                    throw new SelfChatException();
                });
    }

    private void validateMessageRequirements(ChatMessage message) {
        boolean hasRequiredFields = hasRequiredFields(message);
        validateMessageRequirementsCondition(hasRequiredFields);
    }

    private boolean hasRequiredFields(ChatMessage message) {
        boolean hasRoomId = StringUtils.hasText(message.getRoomId());
        boolean hasSenderId = message.getSenderId() != null;
        return hasRoomId && hasSenderId;
    }

    private void validateMessageRequirementsCondition(boolean hasRequiredFields) {
        boolean isInvalid = !hasRequiredFields;
        Optional.of(isInvalid)
                .filter(invalid -> invalid)
                .ifPresent(invalid -> {
                    throw new InvalidChatRequestException();
                });
    }

    private void enrichMessageId(ChatMessage message) {
        Optional.ofNullable(message.getMessageId())
                .or(() -> {
                    String messageId = UUID.randomUUID().toString();
                    message.setMessageId(messageId);
                    return Optional.of(messageId);
                });
    }

    private void enrichTimestamp(ChatMessage message) {
        Optional.ofNullable(message.getTimestamp())
                .or(() -> {
                    LocalDateTime timestamp = LocalDateTime.now();
                    message.setTimestamp(timestamp);
                    return Optional.of(timestamp);
                });
    }

    private void enrichMessageType(ChatMessage message) {
        Optional.ofNullable(message.getType())
                .or(() -> {
                    message.setType(MessageType.CHAT);
                    return Optional.of(MessageType.CHAT);
                });
    }

    private void validateIsGroupChat(ChatRoom room) {
        boolean isNotGroupChat = !room.isGroupChat();
        validateGroupChatCondition(isNotGroupChat);
    }

    private void validateGroupChatCondition(boolean isNotGroupChat) {
        Optional.of(isNotGroupChat)
                .filter(notGroupChat -> notGroupChat)
                .ifPresent(notGroupChat -> {
                    throw new IllegalArgumentException("1:1 채팅방에서는 강퇴할 수 없습니다.");
                });
    }

    private void validateManagerAuthority(ChatRoom room, Long requesterId) {
        Long managerId = room.getManagerId();
        boolean hasNoManager = managerId == null;
        boolean isNotManager = hasNoManager || !managerId.equals(requesterId);
        validateManagerAuthorityCondition(hasNoManager, isNotManager);
    }

    private void validateManagerAuthorityCondition(boolean hasNoManager, boolean isNotManager) {
        boolean isUnauthorized = hasNoManager || isNotManager;
        Optional.of(isUnauthorized)
                .filter(unauthorized -> unauthorized)
                .ifPresent(unauthorized -> {
                    throw new UnauthorizedManagerActionException();
                });
    }

    private void validateUserExistsInRoom(ChatRoom room, Long userToKick) {
        boolean userNotInRoom = !room.getParticipants().contains(userToKick);
        validateUserExistsInRoomCondition(userNotInRoom);
    }

    private void validateUserExistsInRoomCondition(boolean userNotInRoom) {
        Optional.of(userNotInRoom)
                .filter(notInRoom -> notInRoom)
                .ifPresent(notInRoom -> {
                    throw new UserNotInRoomException();
                });
    }

    private void validateNotKickingManager(ChatRoom room, Long userToKick) {
        Long managerId = room.getManagerId();
        boolean isKickingManager = Objects.equals(managerId, userToKick);
        validateNotKickingManagerCondition(isKickingManager);
    }

    private void validateNotKickingManagerCondition(boolean isKickingManager) {
        Optional.of(isKickingManager)
                .filter(kickingManager -> kickingManager)
                .ifPresent(kickingManager -> {
                    throw new ManagerKickAttemptException();
                });
    }
}