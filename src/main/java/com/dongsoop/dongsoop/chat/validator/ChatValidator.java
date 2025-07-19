package com.dongsoop.dongsoop.chat.validator;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.exception.GroupChatOnlyException;
import com.dongsoop.dongsoop.chat.exception.InvalidChatRequestException;
import com.dongsoop.dongsoop.chat.exception.ManagerKickAttemptException;
import com.dongsoop.dongsoop.chat.exception.SelfChatException;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedManagerActionException;
import com.dongsoop.dongsoop.chat.exception.UserKickedException;
import com.dongsoop.dongsoop.chat.exception.UserNotInRoomException;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatSyncService;
import com.dongsoop.dongsoop.chat.util.ChatCommonUtils;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

        validateUserNotKicked(room, userId);
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

        ChatCommonUtils.enrichMessage(message);
        return message;
    }

    public void validateManagerPermission(ChatRoom room, Long requesterId) {
        validateIsGroupChat(room);
        validateIsRoomManager(room, requesterId);
    }

    public void validateKickableUser(ChatRoom room, Long userToKick) {
        validateUserExistsInRoom(room, userToKick);
        validateNotKickingManager(room, userToKick);
    }

    private void validateUserNotKicked(ChatRoom room, Long userId) {
        if (room.isKicked(userId)) {
            throw new UserKickedException(room.getRoomId());
        }
    }

    private void addUserToRoomIfNeeded(ChatRoom room, Long userId) {
        if (shouldAddUserToRoom(userId, room)) {
            addUserToRoom(room, userId);
        }
    }

    private boolean shouldAddUserToRoom(Long userId, ChatRoom room) {
        if (isAnonymousUser(userId)) {
            return false;
        }
        return !isUserAlreadyInRoom(userId, room);
    }

    private boolean isAnonymousUser(Long userId) {
        return ANONYMOUS_USER_ID.equals(userId);
    }

    private boolean isUserAlreadyInRoom(Long userId, ChatRoom room) {
        return room.getParticipants().contains(userId);
    }

    private void addUserToRoom(ChatRoom room, Long userId) {
        room.addNewParticipant(userId);
        chatRepository.saveRoom(room);
    }

    private void validateMessageRequirements(ChatMessage message) {
        validateMessageHasRoomId(message);
        validateMessageHasSenderId(message);
    }

    private void validateMessageHasRoomId(ChatMessage message) {
        if (!StringUtils.hasText(message.getRoomId())) {
            throw new InvalidChatRequestException();
        }
    }

    private void validateMessageHasSenderId(ChatMessage message) {
        if (message.getSenderId() == null) {
            throw new InvalidChatRequestException();
        }
    }

    private void validateIsGroupChat(ChatRoom room) {
        if (!room.isGroupChat()) {
            throw new GroupChatOnlyException("사용자 초대");
        }
    }

    private void validateIsRoomManager(ChatRoom room, Long requesterId) {
        Long managerId = room.getManagerId();
        if (managerId == null) {
            throw new UnauthorizedManagerActionException();
        }
        if (!Objects.equals(managerId, requesterId)) {
            throw new UnauthorizedManagerActionException();
        }
    }

    private void validateUserExistsInRoom(ChatRoom room, Long userToKick) {
        if (!room.getParticipants().contains(userToKick)) {
            throw new UserNotInRoomException();
        }
    }

    private void validateNotKickingManager(ChatRoom room, Long userToKick) {
        if (Objects.equals(room.getManagerId(), userToKick)) {
            throw new ManagerKickAttemptException();
        }
    }
}