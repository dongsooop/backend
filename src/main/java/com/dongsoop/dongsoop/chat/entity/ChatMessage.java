package com.dongsoop.dongsoop.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private String messageId;
    private String roomId;
    private String senderId;
    private String content;
    private LocalDateTime timestamp;
    private MessageType type;

}