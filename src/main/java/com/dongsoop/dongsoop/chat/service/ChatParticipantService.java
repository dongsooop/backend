package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.KickedUserInviteException;
import com.dongsoop.dongsoop.chat.exception.UserAlreadyInRoomException;
import com.dongsoop.dongsoop.chat.util.ChatCommonUtils;
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

    public ChatMessage inviteUserToGroupChat(String roomId, Long inviterId, Long targetUserId,
                                             ChatRoomService chatRoomService, ChatMessageService chatMessageService) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);

        validateInviteRequest(room, inviterId, targetUserId);
        addUserToGroupChatRoom(room, targetUserId, chatRoomService);

        return createInviteSystemMessage(roomId, targetUserId, chatMessageService);
    }

    public ChatRoom kickUserFromRoom(String roomId, Long managerId, Long userToKick,
                                     ChatRoomService chatRoomService, ChatMessageService chatMessageService) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);

        chatValidator.validateManagerPermission(room, managerId);
        chatValidator.validateKickableUser(room, userToKick);

        processUserKickWithMessage(room, roomId, userToKick, chatMessageService);

        return chatRoomService.saveRoom(room);
    }

    public void leaveChatRoom(String roomId, Long userId,
                              ChatRoomService chatRoomService, ChatMessageService chatMessageService) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);

        boolean isContactRoom = room.getTitle() != null && room.getTitle().startsWith("[문의]");

        if (isContactRoom) {
            chatRoomService.handleContactRoomLeave(roomId, userId);
            chatMessageService.createAndSaveSystemMessage(roomId, userId, MessageType.LEAVE);
            return;
        }

        processUserLeaveWithMessage(room, roomId, userId, chatMessageService);
        chatRoomService.saveRoom(room);
        chatRoomService.deleteRoomIfEmpty(room);
    }

    public ChatMessage checkFirstTimeEntryAndCreateEnterMessage(String roomId, Long userId,
                                                                ChatRoomService chatRoomService,
                                                                ChatMessageService chatMessageService) {
        chatValidator.validateUserForRoom(roomId, userId);

        boolean isFirstTime = isFirstTimeEntry(roomId, userId, chatRoomService, chatMessageService);
        if (isFirstTime) {
            return chatMessageService.createAndSaveSystemMessage(roomId, userId, MessageType.ENTER);
        }
        return null;
    }

    public LocalDateTime determineUserJoinTime(ChatRoom room, Long userId, ChatRoomService chatRoomService) {
        LocalDateTime existingJoinTime = room.getJoinTime(userId);

        boolean hasExistingJoinTime = existingJoinTime != null;
        if (hasExistingJoinTime) {
            return existingJoinTime;
        }
        return addNewParticipantAndGetJoinTime(room, userId, chatRoomService);
    }

    private void validateInviteRequest(ChatRoom room, Long inviterId, Long targetUserId) {
        chatValidator.validateManagerPermission(room, inviterId);
        validateTargetUserForInvite(room, targetUserId);
        ChatCommonUtils.validatePositiveUserId(targetUserId);
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

    private void addUserToGroupChatRoom(ChatRoom room, Long userId, ChatRoomService chatRoomService) {
        LocalDateTime joinTime = ChatCommonUtils.getCurrentTime();
        room.addNewParticipant(userId);
        chatRoomService.saveRoom(room);

        readStatusService.initializeUserReadStatus(userId, room.getRoomId(), joinTime);
    }

    private ChatMessage createInviteSystemMessage(String roomId, Long targetUserId,
                                                  ChatMessageService chatMessageService) {
        return chatMessageService.createAndSaveSystemMessage(roomId, targetUserId, MessageType.ENTER);
    }

    private void processUserKickWithMessage(ChatRoom room, String roomId, Long userToKick,
                                            ChatMessageService chatMessageService) {
        room.kickUser(userToKick);
        chatMessageService.createAndSaveSystemMessage(roomId, userToKick, MessageType.LEAVE);
    }

    private void processUserLeaveWithMessage(ChatRoom room, String roomId, Long userId,
                                             ChatMessageService chatMessageService) {
        room.kickUser(userId);
        chatMessageService.createAndSaveSystemMessage(roomId, userId, MessageType.LEAVE);
    }

    private boolean isFirstTimeEntry(String roomId, Long userId, ChatRoomService chatRoomService,
                                     ChatMessageService chatMessageService) {
        ChatRoom room = chatRoomService.getChatRoomById(roomId);
        LocalDateTime userJoinTime = room.getJoinTime(userId);

        boolean noJoinTime = Objects.isNull(userJoinTime);
        if (noJoinTime) {
            return true;
        }
        return isNewInvitedUser(roomId, room, userId, userJoinTime, chatMessageService);
    }

    private boolean isNewInvitedUser(String roomId, ChatRoom room, Long userId, LocalDateTime userJoinTime,
                                     ChatMessageService chatMessageService) {
        LocalDateTime roomCreatedAt = room.getCreatedAt();
        boolean isLaterThanRoomCreation = userJoinTime.isAfter(roomCreatedAt);

        return isLaterThanRoomCreation && hasNotEnteredBefore(roomId, userId, userJoinTime, chatMessageService);
    }

    private boolean hasNotEnteredBefore(String roomId, Long userId, LocalDateTime joinTime,
                                        ChatMessageService chatMessageService) {
        List<ChatMessage> enterMessages = chatMessageService.loadMessagesAfterJoinTime(roomId, joinTime)
                .stream()
                .filter(msg -> msg.getType() == MessageType.ENTER)
                .filter(msg -> msg.getSenderId().equals(userId))
                .toList();

        return enterMessages.isEmpty();
    }

    private LocalDateTime addNewParticipantAndGetJoinTime(ChatRoom room, Long userId, ChatRoomService chatRoomService) {
        LocalDateTime joinTime = ChatCommonUtils.getCurrentTime();
        room.addNewParticipant(userId);
        chatRoomService.saveRoom(room);
        return joinTime;
    }
}