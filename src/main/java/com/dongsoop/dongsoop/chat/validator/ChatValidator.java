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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class ChatValidator {
    private static final Long ANONYMOUS_USER_ID = -1L;

    private final ChatRepository chatRepository;
    private final ChatSyncService chatSyncService;

    public ChatValidator(@Qualifier("redisChatRepository") ChatRepository chatRepository,
                         ChatSyncService chatSyncService,
                         MemberService memberService) {
        this.chatRepository = chatRepository;
        this.chatSyncService = chatSyncService;
    }

    public void validateUserForRoom(String roomId, Long userId) {
        ChatRoom room = findRoomOrRestore(roomId);
        processUserAccess(room, userId);
    }

    public void validateSelfChat(Long user1, Long user2) {
        Optional.of(user1.equals(user2))
                .filter(Boolean::booleanValue)
                .ifPresent(ignored -> {
                    throw new SelfChatException();
                });
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
        Optional.ofNullable(room.getManagerId())
                .filter(managerId -> !managerId.equals(userId))
                .ifPresent(ignored -> {
                    throw new UnauthorizedManagerActionException();
                });
    }

    public void validateKickableUser(ChatRoom room, Long userToKick) {
        validateUserExistsInRoom(room, userToKick);
        validateNotKickingManager(room, userToKick);
    }

    private ChatRoom findRoomOrRestore(String roomId) {
        return chatRepository.findRoomById(roomId)
                .orElseGet(() -> restoreRoomFromDatabase(roomId));
    }

    private ChatRoom restoreRoomFromDatabase(String roomId) {
        return Optional.ofNullable(chatSyncService.restoreGroupChatRoom(roomId))
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    private void processUserAccess(ChatRoom room, Long userId) {
        handleAnonymousUser(userId);
        handleKickedUser(room, userId);
        handleParticipantAddition(room, userId);
    }

    private void handleAnonymousUser(Long userId) {
        Optional.of(ANONYMOUS_USER_ID.equals(userId))
                .filter(Boolean::booleanValue)
                .ifPresent(ignored -> {
                });
    }

    private void handleKickedUser(ChatRoom room, Long userId) {
        Optional.of(room.isKicked(userId))
                .filter(Boolean::booleanValue)
                .ifPresent(ignored -> {
                    throw new UserKickedException(room.getRoomId());
                });
    }

    private void handleParticipantAddition(ChatRoom room, Long userId) {
        Optional.of(userId)
                .filter(id -> !ANONYMOUS_USER_ID.equals(id))
                .filter(id -> !room.getParticipants().contains(id))
                .ifPresent(id -> addUserToRoom(room, id));
    }

    private void addUserToRoom(ChatRoom room, Long userId) {
        room.getParticipants().add(userId);
        chatRepository.saveRoom(room);
    }

    private void validateMessageRequirements(ChatMessage message) {
        Optional.ofNullable(message)
                .filter(this::hasRequiredFields)
                .orElseThrow(InvalidChatRequestException::new);
    }

    private boolean hasRequiredFields(ChatMessage message) {
        return StringUtils.hasText(message.getRoomId()) &&
                message.getSenderId() != null;
    }

    private ChatMessage enrichMessageData(ChatMessage message) {
        setMessageIdIfAbsent(message);
        setTimestampIfAbsent(message);
        setMessageTypeIfAbsent(message);
        setSenderNickNameIfAbsent(message);
        return message;
    }

    private void setMessageIdIfAbsent(ChatMessage message) {
        Optional.ofNullable(message.getMessageId())
                .orElseGet(() -> {
                    String newId = UUID.randomUUID().toString();
                    message.setMessageId(newId);
                    return newId;
                });
    }

    private void setTimestampIfAbsent(ChatMessage message) {
        Optional.ofNullable(message.getTimestamp())
                .orElseGet(() -> {
                    LocalDateTime now = LocalDateTime.now();
                    message.setTimestamp(now);
                    return now;
                });
    }

    private void setMessageTypeIfAbsent(ChatMessage message) {
        Optional.ofNullable(message.getType())
                .orElseGet(() -> {
                    message.setType(MessageType.CHAT);
                    return MessageType.CHAT;
                });
    }

    private void setSenderNickNameIfAbsent(ChatMessage message) {
        Optional.ofNullable(message.getSenderNickName())
                .orElseGet(() -> {
                    String defaultName = "사용자" + message.getSenderId();
                    message.setSenderNickName(defaultName);
                    return defaultName;
                });
    }

    private Set<String> extractMessageIds(List<ChatMessage> messages) {
        return messages.stream()
                .map(ChatMessage::getMessageId)
                .collect(Collectors.toSet());
    }

    private List<ChatMessage> filterNewMessages(List<ChatMessage> clientMessages, Set<String> existingIds) {
        Predicate<ChatMessage> isNewMessage = msg -> !existingIds.contains(msg.getMessageId());

        return clientMessages.stream()
                .filter(isNewMessage)
                .toList();
    }

    private void validateUserExistsInRoom(ChatRoom room, Long userToKick) {
        Optional.of(!room.getParticipants().contains(userToKick))
                .filter(Boolean::booleanValue)
                .ifPresent(ignored -> {
                    throw new UserNotInRoomException();
                });
    }

    private void validateNotKickingManager(ChatRoom room, Long userToKick) {
        Optional.ofNullable(room.getManagerId())
                .filter(managerId -> managerId.equals(userToKick))
                .ifPresent(ignored -> {
                    throw new ManagerKickAttemptException();
                });
    }
}