package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.KickedUserInviteException;
import com.dongsoop.dongsoop.chat.exception.UserAlreadyInRoomException;
import com.dongsoop.dongsoop.chat.util.ChatMessageUtils;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatParticipantService {
    private final ChatValidator chatValidator;
    private final ReadStatusService readStatusService;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;

    public ChatMessage inviteUserToGroupChat(String roomId, Long inviterId, Long targetUserId) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);

        validateInviteRequest(room, inviterId, targetUserId);
        addUserToGroupChatRoom(room, targetUserId);

        return createInviteSystemMessage(roomId, targetUserId);
    }

    public ChatRoom kickUserFromRoom(String roomId, Long managerId, Long userToKick) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);

        chatValidator.validateManagerPermission(room, managerId);
        chatValidator.validateKickableUser(room, userToKick);

        processUserKickWithMessage(room, roomId, userToKick);

        return chatRoomService.saveRoom(room);
    }

    public void leaveChatRoom(String roomId, Long userId) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);

        boolean isContactRoom = room.getTitle() != null && room.getTitle().startsWith("[문의]");

        if (isContactRoom) {
            chatRoomService.handleContactRoomLeave(roomId, userId);
            chatMessageService.createAndSaveSystemMessage(roomId, userId, MessageType.LEAVE);
            return;
        }

        processUserLeaveWithMessage(room, roomId, userId);
        chatRoomService.saveRoom(room);
        chatRoomService.deleteRoomIfEmpty(room);
    }

    public ChatMessage checkFirstTimeEntryAndCreateEnterMessage(String roomId, Long userId) {
        chatValidator.validateUserForRoom(roomId, userId);

        boolean isFirstTime = isFirstTimeEntry(roomId, userId);
        if (isFirstTime) {
            return chatMessageService.createAndSaveSystemMessage(roomId, userId, MessageType.ENTER);
        }
        return null;
    }

    public LocalDateTime determineUserJoinTime(ChatRoom room, Long userId) {
        LocalDateTime existingJoinTime = room.getJoinTime(userId);

        boolean hasExistingJoinTime = existingJoinTime != null;
        if (hasExistingJoinTime) {
            return existingJoinTime;
        }
        return addNewParticipantAndGetJoinTime(room, userId);
    }

    private void validateInviteRequest(ChatRoom room, Long inviterId, Long targetUserId) {
        chatValidator.validateManagerPermission(room, inviterId);
        validateTargetUserForInvite(room, targetUserId);
        ChatMessageUtils.validatePositiveUserId(targetUserId);
    }

    private void validateTargetUserForInvite(ChatRoom room, Long targetUserId) {
        boolean userAlreadyExists = room.getParticipants().contains(targetUserId);
        if (userAlreadyExists) {
            throw new UserAlreadyInRoomException(targetUserId);
        }

        boolean userWasKicked = room.isKicked(targetUserId);
        if (userWasKicked) {
            throw new KickedUserInviteException(targetUserId);
        }
    }

    private void addUserToGroupChatRoom(ChatRoom room, Long userId) {
        LocalDateTime joinTime = ChatMessageUtils.getCurrentTime();
        room.addNewParticipant(userId);
        chatRoomService.saveRoom(room);

        readStatusService.initializeUserReadStatus(userId, room.getRoomId(), joinTime);
    }

    private ChatMessage createInviteSystemMessage(String roomId, Long targetUserId) {
        return chatMessageService.createAndSaveSystemMessage(roomId, targetUserId, MessageType.ENTER);
    }

    private void processUserKickWithMessage(ChatRoom room, String roomId, Long userToKick) {
        room.kickUser(userToKick);
        chatMessageService.createAndSaveSystemMessage(roomId, userToKick, MessageType.LEAVE);
    }

    private void processUserLeaveWithMessage(ChatRoom room, String roomId, Long userId) {
        room.kickUser(userId);
        chatMessageService.createAndSaveSystemMessage(roomId, userId, MessageType.LEAVE);
    }

    private boolean isFirstTimeEntry(String roomId, Long userId) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);
        LocalDateTime userJoinTime = room.getJoinTime(userId);

        boolean noJoinTime = Objects.isNull(userJoinTime);
        if (noJoinTime) {
            return true;
        }
        return isNewInvitedUser(roomId, room, userId, userJoinTime);
    }

    private boolean isNewInvitedUser(String roomId, ChatRoom room, Long userId, LocalDateTime userJoinTime) {
        LocalDateTime roomCreatedAt = room.getCreatedAt();
        boolean isLaterThanRoomCreation = userJoinTime.isAfter(roomCreatedAt);

        return isLaterThanRoomCreation && hasNotEnteredBefore(roomId, userId, userJoinTime);
    }

    private boolean hasNotEnteredBefore(String roomId, Long userId, LocalDateTime joinTime) {
        List<ChatMessage> enterMessages = chatMessageService.loadMessagesAfterJoinTime(roomId, joinTime)
                .stream()
                .filter(msg -> msg.getType() == MessageType.ENTER)
                .filter(msg -> msg.getSenderId().equals(userId))
                .toList();

        return enterMessages.isEmpty();
    }

    private LocalDateTime addNewParticipantAndGetJoinTime(ChatRoom room, Long userId) {
        LocalDateTime joinTime = ChatMessageUtils.getCurrentTime();
        room.addNewParticipant(userId);
        chatRoomService.saveRoom(room);
        return joinTime;
    }
}
