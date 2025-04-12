package com.dongsoop.dongsoop.chat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEntity {
    @Id
    private String messageId;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String senderId;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private MessageType type;

    public ChatMessage toChatMessage() {
        return ChatMessage.builder()
                .messageId(this.messageId)
                .roomId(this.roomId)
                .senderId(this.senderId)
                .content(this.content)
                .timestamp(this.timestamp)
                .type(this.type)
                .build();
    }
}