package com.dongsoop.dongsoop.chat.dto;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatNotificationType;

import java.time.LocalDateTime;

public record ChatRoomUpdateDto(
        ChatNotificationType type,
        String roomId,
        String lastMessage,
        LocalDateTime timestamp,
        Long senderId,
        Integer unreadCount
) {
    public static ChatRoomUpdateDto createRoomUpdate(String roomId, ChatMessage message, Integer unreadCount) {
        return new ChatRoomUpdateDto(
                ChatNotificationType.ROOM_UPDATE,
                roomId,
                message.getContent(),
                message.getTimestamp(),
                message.getSenderId(),
                unreadCount
        );
    }
}