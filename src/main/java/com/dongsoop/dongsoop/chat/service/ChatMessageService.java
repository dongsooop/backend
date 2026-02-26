package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.util.ChatMessageUtils;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final RedisChatRepository redisChatRepository;
    private final ChatValidator chatValidator;
    private final MemberService memberService;

    public ChatMessage processMessage(ChatMessage message) {
        ChatMessage enrichedMessage = chatValidator.validateAndEnrichMessage(message);
        redisChatRepository.saveMessage(enrichedMessage);
        return enrichedMessage;
    }

    public ChatMessage processWebSocketMessage(ChatMessage message, Long userId, String roomId) {
        ChatMessage enrichedMessage = enrichMessageWithUserData(message, userId, roomId);
        return processMessage(enrichedMessage);
    }

    public ChatMessage createAndSaveSystemMessage(String roomId, Long userId, MessageType type) {
        ChatMessage message = buildSystemMessage(roomId, userId, type);
        redisChatRepository.saveMessage(message);
        return message;
    }

    public List<ChatMessage> processOfflineMessages(String roomId, Long userId, List<ChatMessage> offlineMessages) {
        return offlineMessages.stream()
                .map(message -> processOfflineMessage(message, userId, roomId))
                .filter(Objects::nonNull)
                .toList();
    }

    public ChatMessage findMessageById(List<ChatMessage> messages, String messageId) {
        for (ChatMessage message : messages) {
            if (messageId.equals(message.getMessageId())) {
                return message;
            }
        }
        return null;
    }

    public List<ChatMessage> loadMessagesAfterJoinTime(String roomId, LocalDateTime userJoinTime) {
        return redisChatRepository.findMessagesByRoomIdAfterTime(roomId, userJoinTime);
    }

    public List<ChatMessage> loadNewMessages(String roomId, String lastMessageId) {
        boolean hasLastMessageId = lastMessageId != null;
        if (hasLastMessageId) {
            return redisChatRepository.findMessagesByRoomIdAfterId(roomId, lastMessageId);
        }
        return redisChatRepository.findMessagesByRoomId(roomId);
    }

    public int countUnreadMessages(List<ChatMessage> messages, Long userId) {
        return ChatMessage.countUnreadMessages(messages, userId);
    }

    public List<ChatMessage> getAllMessages(String roomId) {
        return redisChatRepository.findMessagesByRoomId(roomId);
    }

    public List<ChatMessage> getMessagesAfterId(String roomId, String messageId) {
        return redisChatRepository.findMessagesByRoomIdAfterId(roomId, messageId);
    }

    public Map<String, String> getLastMessageTextsBatch(List<String> roomIds) {
        Map<String, ChatMessage> lastMessages = redisChatRepository.findLastMessagesByRoomIds(roomIds);
        Map<String, String> result = new HashMap<>();
        for (String roomId : roomIds) {
            ChatMessage message = lastMessages.get(roomId);
            result.put(roomId, message != null ? message.getContent() : null);
        }
        return result;
    }

    public String getLastMessageText(String roomId) {
        ChatMessage lastMessage = redisChatRepository.findLastMessageByRoomId(roomId);

        if (lastMessage == null) {
            return null;
        }

        return lastMessage.getContent();
    }

    private ChatMessage buildSystemMessage(String roomId, Long userId, MessageType type) {
        String content = createSystemMessageContent(userId, type);

        return ChatMessage.builder()
                .messageId(ChatMessageUtils.generateMessageId())
                .roomId(roomId)
                .senderId(userId)
                .content(content)
                .timestamp(ChatMessageUtils.getCurrentTime())
                .type(type)
                .build();
    }

    private String createSystemMessageContent(Long userId, MessageType type) {
        String nickname = memberService.getNicknameById(userId);

        if (type == MessageType.LEAVE) {
            return nickname + "님이 나갔습니다.";
        }

        if (type == MessageType.ENTER) {
            return nickname + "님이 입장했습니다.";
        }

        return nickname;
    }

    private ChatMessage enrichMessageWithUserData(ChatMessage message, Long userId, String roomId) {
        message.setSenderId(userId);
        message.setRoomId(roomId);
        return message;
    }

    private ChatMessage processOfflineMessage(ChatMessage message, Long userId, String roomId) {
        boolean messageNull = message == null;
        if (messageNull) {
            return null;
        }

        ChatMessage enrichedMessage = enrichOfflineMessage(message, userId, roomId);
        return validateAndSaveOfflineMessage(enrichedMessage);
    }

    private ChatMessage enrichOfflineMessage(ChatMessage message, Long userId, String roomId) {
        message.setSenderId(userId);
        message.setRoomId(roomId);

        ChatMessageUtils.enrichMessage(message);

        return message;
    }

    private ChatMessage validateAndSaveOfflineMessage(ChatMessage message) {
        redisChatRepository.saveMessage(message);
        return message;
    }
}