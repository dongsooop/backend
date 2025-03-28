package com.dongsoop.dongsoop.chat.entity;

import lombok.*;
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
