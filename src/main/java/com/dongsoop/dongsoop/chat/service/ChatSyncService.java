package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessageEntity;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomEntity;
import com.dongsoop.dongsoop.chat.repository.ChatMessageJpaRepository;
import com.dongsoop.dongsoop.chat.repository.ChatRoomJpaRepository;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.exception.domain.websocket.ChatRoomNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSyncService {
    private final RedisChatRepository redisChatRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ChatMessageJpaRepository chatMessageJpaRepository;

    public ChatRoom findRoomOrRestore(String roomId) {
        ChatRoom room = redisChatRepository.findRoomById(roomId).orElse(null);

        if (room != null) {
            return room;
        }

        ChatRoom restoredRoom = restoreGroupChatRoom(roomId);
        if (restoredRoom == null) {
            throw new ChatRoomNotFoundException();
        }
        return restoredRoom;
    }

    public ChatRoom restoreGroupChatRoom(String roomId) {
        return chatRoomJpaRepository.findById(roomId)
                .map(this::restoreRoomToRedis)
                .orElse(null);
    }

    private ChatRoom restoreRoomToRedis(ChatRoomEntity entity) {
        ChatRoom room = entity.toChatRoom();
        redisChatRepository.saveRoom(room);
        restoreMessagesToRedis(entity.getRoomId());
        return room;
    }

    private void restoreMessagesToRedis(String roomId) {
        chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId)
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .forEach(redisChatRepository::saveMessage);
    }
}