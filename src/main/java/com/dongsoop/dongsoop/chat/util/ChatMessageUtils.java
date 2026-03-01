package com.dongsoop.dongsoop.chat.util;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;

import java.time.LocalDateTime;
import java.util.UUID;

public final class ChatMessageUtils {

    private ChatMessageUtils() {
    }

    // userId가 null이거나 0 이하이면 접근 거부
    public static void validatePositiveUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new UnauthorizedChatAccessException();
        }
    }

    public static String generateMessageId() {
        return UUID.randomUUID().toString();
    }

    public static void enrichMessageId(ChatMessage message) {
        String currentMessageId = message.getMessageId();

        if (currentMessageId == null || currentMessageId.trim().isEmpty()) {
            message.setMessageId(generateMessageId());
        }
    }

    public static void enrichMessageTimestamp(ChatMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
    }

    public static void enrichMessageType(ChatMessage message) {
        if (message.getType() == null) {
            message.setType(MessageType.CHAT);
        }
    }

    // message null이면 무시
    public static void enrichMessage(ChatMessage message) {
        if (message == null) {
            return;
        }
        enrichMessageId(message);
        enrichMessageTimestamp(message);
        enrichMessageType(message);
    }
}
