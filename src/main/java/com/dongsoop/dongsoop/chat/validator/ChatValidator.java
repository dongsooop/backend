package com.dongsoop.dongsoop.chat.validator;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.exception.domain.websocket.ChatRoomNotFoundException;
import com.dongsoop.dongsoop.exception.domain.websocket.ChatRoomValidationException;
import com.dongsoop.dongsoop.exception.domain.websocket.EmptyParticipantsException;
import com.dongsoop.dongsoop.exception.domain.websocket.InvalidChatRequestException;
import com.dongsoop.dongsoop.exception.domain.websocket.SelfChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatValidator {
    private final ChatRepository chatRepository;

    public void validateUserForRoom(String roomId, String userId) {
        log.info("사용자 {} 채팅방 {} 권한 검증", userId, roomId);
        ensureUserHasAccess(roomId, userId);
    }

    public void validateSelfChat(String user1, String user2) {
        if (user1.equals(user2)) {
            throw new SelfChatException();
        }
    }

    public ChatMessage validateAndEnrichMessage(ChatMessage message) {
        validateMessageFormat(message);
        ensureUserHasAccess(message.getRoomId(), message.getSenderId());
        return enrichMessage(message);
    }

    public List<ChatMessage> filterDuplicateMessages(List<ChatMessage> serverMessages,
                                                     List<ChatMessage> clientMessages) {
        Set<String> existingMessageIds = extractMessageIds(serverMessages);
        return filterNewMessages(clientMessages, existingMessageIds);
    }

    // 헬퍼 메소드
    private void ensureUserHasAccess(String roomId, String userId) {
        ChatRoom room = findRoom(roomId);
        validateUser(userId);
        addUserToRoomIfNeeded(room, userId);
        ensureUserInParticipants(room, userId);
    }

    private ChatRoom findRoom(String roomId) {
        return chatRepository.findRoomById(roomId)
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    private void validateUser(String userId) {
        if ("anonymousUser".equals(userId)) {
            log.warn("익명 사용자 접근 시도: {}", userId);
            throw new ChatRoomValidationException("로그인이 필요합니다.");
        }
    }

    private void addUserToRoomIfNeeded(ChatRoom room, String userId) {
        boolean userAdded = room.getParticipants().add(userId);
        if (userAdded) {
            log.info("사용자 {}를 채팅방 {} 참가자 목록에 추가", userId, room.getRoomId());
            chatRepository.saveRoom(room);
        }
    }

    private void ensureUserInParticipants(ChatRoom room, String userId) {
        if (!room.getParticipants().contains(userId)) {
            log.error("사용자 {}가 채팅방 {} 참가자 목록에 없음", userId, room.getRoomId());
            throw new ChatRoomValidationException("해당 채팅방에 참여할 권한이 없습니다.");
        }
    }

    private void validateMessageFormat(ChatMessage message) {
        if (message == null) {
            throw new InvalidChatRequestException();
        }

        if (!StringUtils.hasText(message.getRoomId()) || !StringUtils.hasText(message.getSenderId())) {
            throw new InvalidChatRequestException();
        }
    }

    private ChatMessage enrichMessage(ChatMessage message) {
        if (message.getMessageId() == null) {
            message.setMessageId(UUID.randomUUID().toString());
        }

        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }

        return message;
    }

    private Set<String> extractMessageIds(List<ChatMessage> messages) {
        return messages.stream()
                .map(ChatMessage::getMessageId)
                .collect(Collectors.toSet());
    }

    private List<ChatMessage> filterNewMessages(List<ChatMessage> messages, Set<String> existingIds) {
        return messages.stream()
                .filter(msg -> !existingIds.contains(msg.getMessageId()))
                .collect(Collectors.toList());
    }
}