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
        return restoreRoomFromDatabase(roomId);
    }

    public ChatRoom restoreGroupChatRoom(String roomId) {
        ChatRoomEntity entity = chatRoomJpaRepository.findById(roomId).orElse(null);

        if (entity == null) {
            return null;
        }
        return restoreRoomToRedis(entity);
    }

    private ChatRoom restoreRoomFromDatabase(String roomId) {
        ChatRoom restoredRoom = restoreGroupChatRoom(roomId);

        if (restoredRoom == null) {
            throw new ChatRoomNotFoundException();
        }
        return restoredRoom;
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