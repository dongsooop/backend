package com.dongsoop.dongsoop.chat.util;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import java.time.LocalDateTime;
import java.util.UUID;

public final class ChatCommonUtils {

    private ChatCommonUtils() {
    }

    public static void validatePositiveUserId(Long userId) {
        if (userId == null) {
            throw new UnauthorizedChatAccessException();
        }
        if (userId < 0) {
            throw new UnauthorizedChatAccessException();
        }
    }

    public static String generateMessageId() {
        return UUID.randomUUID().toString();
    }

    public static LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    public static String createSystemMessageContent(Long userId) {
        return userId.toString();
    }

    public static void enrichMessageId(ChatMessage message) {
        String currentMessageId = message.getMessageId();

        if (currentMessageId == null || currentMessageId.isEmpty()) {
            message.setMessageId(generateMessageId());
        }
    }

    public static void enrichMessageTimestamp(ChatMessage message) {
        if (message.getTimestamp() == null) {
            message.setTimestamp(getCurrentTime());
        }
    }

    public static void enrichMessageType(ChatMessage message) {
        if (message.getType() == null) {
            message.setType(MessageType.CHAT);
        }
    }

    public static void enrichMessage(ChatMessage message) {
        enrichMessageId(message);
        enrichMessageTimestamp(message);
        enrichMessageType(message);
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
}