package com.dongsoop.dongsoop.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomInitResponse {
    private ChatRoom room;
    private List<ChatMessage> messages;
    private LocalDateTime userJoinTime;
    private int totalMessageCount;
}