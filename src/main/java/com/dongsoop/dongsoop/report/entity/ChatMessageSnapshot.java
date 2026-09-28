package com.dongsoop.dongsoop.report.entity;

import java.time.LocalDateTime;

public record ChatMessageSnapshot(Long senderId, String content, LocalDateTime sentAt) {

    // message_content 컬럼과 chat_messages.content 길이에 맞춘다
    private static final int MAX_CONTENT_LENGTH = 1000;

    public static ChatMessageSnapshot of(Long senderId, String content, LocalDateTime sentAt) {
        return new ChatMessageSnapshot(senderId, truncate(content), sentAt);
    }

    public static String truncate(String content) {
        if (content == null) {
            return "";
        }

        if (content.length() <= MAX_CONTENT_LENGTH) {
            return content;
        }

        return content.substring(0, MAX_CONTENT_LENGTH);
    }
}
