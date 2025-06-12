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
        ChatRoom room = findRoomInRedis(roomId);

        return Optional.ofNullable(room)
                .orElseGet(() -> restoreRoomFromDatabase(roomId));
    }

    public ChatRoom restoreGroupChatRoom(String roomId) {
        return chatRoomJpaRepository.findById(roomId)
                .map(this::restoreRoomToRedis)
                .orElse(null);
    }

    private ChatRoom findRoomInRedis(String roomId) {
        return redisChatRepository.findRoomById(roomId).orElse(null);
    }

    private ChatRoom restoreRoomFromDatabase(String roomId) {
        ChatRoom restoredRoom = restoreGroupChatRoom(roomId);
        validateRoomExists(restoredRoom);
        return restoredRoom;
    }

    private void validateRoomExists(ChatRoom room) {
        Optional.ofNullable(room)
                .orElseThrow(ChatRoomNotFoundException::new);
    }

    private ChatRoom restoreRoomToRedis(ChatRoomEntity entity) {
        ChatRoom room = convertEntityToRoom(entity);
        saveRoomToRedis(room);
        restoreMessagesToRedis(entity.getRoomId());
        return room;
    }

    private ChatRoom convertEntityToRoom(ChatRoomEntity entity) {
        return entity.toChatRoom();
    }

    private void saveRoomToRedis(ChatRoom room) {
        redisChatRepository.saveRoom(room);
    }

    private void restoreMessagesToRedis(String roomId) {
        List<ChatMessageEntity> messageEntities = loadMessageEntitiesFromDatabase(roomId);
        saveMessagesToRedis(messageEntities);
    }

    private List<ChatMessageEntity> loadMessageEntitiesFromDatabase(String roomId) {
        return chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    private void saveMessagesToRedis(List<ChatMessageEntity> messageEntities) {
        messageEntities.stream()
                .map(ChatMessageEntity::toChatMessage)
                .forEach(redisChatRepository::saveMessage);
    }
}