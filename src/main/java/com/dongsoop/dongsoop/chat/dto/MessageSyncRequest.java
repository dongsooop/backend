package com.dongsoop.dongsoop.chat.dto;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MessageSyncRequest {
    private List<ChatMessage> messages;
}