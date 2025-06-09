package com.dongsoop.dongsoop.chat.validator;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatSyncService;
import com.dongsoop.dongsoop.exception.domain.websocket.*;
import com.dongsoop.dongsoop.member.service.MemberService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ChatValidator {
    private static final Long ANONYMOUS_USER_ID = -1L;

    private final ChatRepository chatRepository;
    private final ChatSyncService chatSyncService;
    private final MemberService memberService;

    public ChatValidator(@Qualifier("redisChatRepository") ChatRepository chatRepository,
                         ChatSyncService chatSyncService,
                         MemberService memberService) {
        this.chatRepository = chatRepository;
        this.chatSyncService = chatSyncService;
        this.memberService = memberService;
    }

    public void validateUserForRoom(String roomId, Long userId) {
        ChatRoom room = findRoomOrRestore(roomId);
        processUserAccess(room, userId);
    }

    public void validateSelfChat(Long user1, Long user2) {
        checkSelfChatAttempt(user1, user2);
    }

    public ChatMessage validateAndEnrichMessage(ChatMessage message) {
        validateMessageRequirements(message);
        validateUserForRoom(message.getRoomId(), message.getSenderId());
        return enrichMessageData(message);
    }

    public List<ChatMessage> filterDuplicateMessages(List<ChatMessage> serverMessages, List<ChatMessage> clientMessages) {
        Set<String> existingIds = extractMessageIds(serverMessages);
        return filterNewMessages(clientMessages, existingIds);
    }

    public void validateManagerPermission(ChatRoom room, Long userId) {
        Long managerId = room.getManagerId();
        checkManagerPermission(managerId, userId);
    }

    public void validateKickableUser(ChatRoom room, Long userToKick) {
        validateUserExistsInRoom(room, userToKick);
        validateNotKickingManager(room, userToKick);
    }

    private ChatRoom findRoomOrRestore(String roomId) {
        ChatRoom room = chatRepository.findRoomById(roomId).orElse(null);
        return getValidRoom(room, roomId);
    }

    private ChatRoom getValidRoom(ChatRoom room, String roomId) {
        if (room != null) {
            return room;
        }
        return restoreRoomFromDatabase(roomId);
    }

    private ChatRoom restoreRoomFromDatabase(String roomId) {
        ChatRoom restoredRoom = chatSyncService.restoreGroupChatRoom(roomId);
        validateRoomNotNull(restoredRoom);
        return restoredRoom;
    }

    private void validateRoomNotNull(ChatRoom room) {
        if (room == null) {
            throw new ChatRoomNotFoundException();
        }
    }

    private void checkSelfChatAttempt(Long user1, Long user2) {
        if (user1.equals(user2)) {
            throw new SelfChatException();
        }
    }

    private void processUserAccess(ChatRoom room, Long userId) {
        handleKickedUser(room, userId);
        handleParticipantAddition(room, userId);
    }

    private void handleKickedUser(ChatRoom room, Long userId) {
        if (room.isKicked(userId)) {
            throw new UserKickedException(room.getRoomId());
        }
    }

    private void handleParticipantAddition(ChatRoom room, Long userId) {
        boolean shouldAddUser = shouldAddUserToRoom(userId, room);
        if (shouldAddUser) {
            addUserToRoom(room, userId);
        }
    }

    private boolean shouldAddUserToRoom(Long userId, ChatRoom room) {
        return !ANONYMOUS_USER_ID.equals(userId) && !room.getParticipants().contains(userId);
    }

    private void addUserToRoom(ChatRoom room, Long userId) {
        room.getParticipants().add(userId);
        chatRepository.saveRoom(room);
    }

    private void validateMessageRequirements(ChatMessage message) {
        boolean hasRequiredFields = hasRequiredFields(message);
        if (!hasRequiredFields) {
            throw new InvalidChatRequestException();
        }
    }

    private boolean hasRequiredFields(ChatMessage message) {
        return StringUtils.hasText(message.getRoomId()) && message.getSenderId() != null;
    }

    private ChatMessage enrichMessageData(ChatMessage message) {
        setMessageIdIfAbsent(message);
        setTimestampIfAbsent(message);
        setMessageTypeIfAbsent(message);
        setSenderNickNameIfAbsent(message);
        return message;
    }

    private void setMessageIdIfAbsent(ChatMessage message) {
        if (message.getMessageId() == null) {
            String newId = UUID.randomUUID().toString();
            message.setMessageId(newId);
        }
    }

    private void setTimestampIfAbsent(ChatMessage message) {
        if (message.getTimestamp() == null) {
            LocalDateTime now = LocalDateTime.now();
            message.setTimestamp(now);
        }
    }

    private void setMessageTypeIfAbsent(ChatMessage message) {
        if (message.getType() == null) {
            message.setType(MessageType.CHAT);
        }
    }

    private void setSenderNickNameIfAbsent(ChatMessage message) {  // 수정
        if (message.getSenderNickName() == null) {
            String nickname = getUserNicknameById(message.getSenderId());
            message.setSenderNickName(nickname);
        }
    }

    private String getUserNicknameById(Long userId) {  // 새 메서드
        return memberService.getNicknameById(userId);
    }

    private Set<String> extractMessageIds(List<ChatMessage> messages) {
        return messages.stream()
                .map(ChatMessage::getMessageId)
                .collect(Collectors.toSet());
    }

    private List<ChatMessage> filterNewMessages(List<ChatMessage> clientMessages, Set<String> existingIds) {
        return clientMessages.stream()
                .filter(message -> isNewMessage(message, existingIds))
                .toList();
    }

    private boolean isNewMessage(ChatMessage message, Set<String> existingIds) {
        return !existingIds.contains(message.getMessageId());
    }

    private void checkManagerPermission(Long managerId, Long userId) {
        if (managerId != null && !managerId.equals(userId)) {
            throw new UnauthorizedManagerActionException();
        }
    }

    private void validateUserExistsInRoom(ChatRoom room, Long userToKick) {
        boolean userNotInRoom = !room.getParticipants().contains(userToKick);
        if (userNotInRoom) {
            throw new UserNotInRoomException();
        }
    }

    private void validateNotKickingManager(ChatRoom room, Long userToKick) {
        Long managerId = room.getManagerId();
        if (managerId != null && managerId.equals(userToKick)) {
            throw new ManagerKickAttemptException();
        }
    }
}