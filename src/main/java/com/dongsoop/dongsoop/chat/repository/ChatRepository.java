package com.dongsoop.dongsoop.chat.repository;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;

import java.util.List;
import java.util.Optional;

public interface ChatRepository {
    ChatRoom saveRoom(ChatRoom room);

    Optional<ChatRoom> findRoomById(String roomId);

    Optional<ChatRoom> findRoomByParticipants(Long user1, Long user2);

    void saveMessage(ChatMessage message);

    List<ChatMessage> findMessagesByRoomId(String roomId);

    List<ChatRoom> findRoomsByUserId(Long userId);
}