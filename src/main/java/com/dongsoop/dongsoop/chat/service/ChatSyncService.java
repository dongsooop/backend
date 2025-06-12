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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatSyncService {
    private final RedisChatRepository redisChatRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ChatMessageJpaRepository chatMessageJpaRepository;

    public ChatRoom findRoomOrRestore(String roomId) {
        ChatRoom room = redisChatRepository.findRoomById(roomId).orElse(null);

        return Optional.ofNullable(room)
                .orElseGet(() -> restoreRoomFromDatabase(roomId));
    }

    public ChatRoom restoreGroupChatRoom(String roomId) {
        return chatRoomJpaRepository.findById(roomId)
                .map(this::restoreRoomToRedis)
                .orElse(null);
    }

    private ChatRoom restoreRoomFromDatabase(String roomId) {
        ChatRoom restoredRoom = restoreGroupChatRoom(roomId);
        
        return Optional.ofNullable(restoredRoom)
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    private ChatRoom restoreRoomToRedis(ChatRoomEntity entity) {
        ChatRoom room = entity.toChatRoom();
        redisChatRepository.saveRoom(room);
        restoreMessagesToRedis(entity.getRoomId());

        return room;
    }

    private void restoreMessagesToRedis(String roomId) {
        List<ChatMessageEntity> messageEntities = chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId);

        messageEntities.stream()
                .map(ChatMessageEntity::toChatMessage)
                .forEach(redisChatRepository::saveMessage);
    }
}