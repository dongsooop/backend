package com.dongsoop.dongsoop.chat.entity;

import java.util.List;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String messageId;
    private String roomId;
    private Long senderId;
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;

    public static List<ChatMessage> excludeUserMessages(List<ChatMessage> messages, Long userId) {
        return messages.stream()
                .filter(msg -> !msg.getSenderId().equals(userId))
                .toList();
    }

    public static int countUnreadMessages(List<ChatMessage> messages, Long userId) {
        return excludeUserMessages(messages, userId).size();
    }
}