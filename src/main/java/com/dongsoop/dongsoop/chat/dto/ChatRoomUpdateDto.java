package com.dongsoop.dongsoop.chat.dto;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;

import java.time.LocalDateTime;

public record ChatRoomUpdateDto(
        String type,
        String roomId,
        String lastMessage,
        LocalDateTime timestamp,
        Long senderId
) {
    public static ChatRoomUpdateDto createRoomUpdate(String roomId, ChatMessage message) {
        return new ChatRoomUpdateDto(
                "ROOM_UPDATE",
                roomId,
                message.getContent(),
                message.getTimestamp(),
                message.getSenderId()
        );
    }
}