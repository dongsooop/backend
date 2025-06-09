package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatMessageEntity;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomEntity;
import com.dongsoop.dongsoop.chat.repository.ChatMessageJpaRepository;
import com.dongsoop.dongsoop.chat.repository.ChatRoomJpaRepository;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatSyncService {
    private final RedisChatRepository redisChatRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ChatMessageJpaRepository chatMessageJpaRepository;

    public ChatRoom restoreGroupChatRoom(String roomId) {
        return chatRoomJpaRepository.findById(roomId)
                .filter(ChatRoomEntity::isGroupChat)
                .map(this::restoreRoomToRedis)
                .orElse(null);
    }

    public List<ChatMessage> restoreMessagesFromDatabase(String roomId) {
        List<ChatMessageEntity> messageEntities = findMessageEntitiesByRoomId(roomId);

        return Optional.of(messageEntities)
                .filter(entities -> !entities.isEmpty())
                .map(this::convertAndSaveMessages)
                .orElse(Collections.emptyList());
    }

    private ChatRoom restoreRoomToRedis(ChatRoomEntity entity) {
        ChatRoom room = entity.toChatRoom();
        redisChatRepository.saveRoom(room);
        restoreMessagesToRedis(entity.getRoomId());
        return room;
    }

    private void restoreMessagesToRedis(String roomId) {
        findMessageEntitiesByRoomId(roomId)
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .forEach(redisChatRepository::saveMessage);
    }

    private List<ChatMessageEntity> findMessageEntitiesByRoomId(String roomId) {
        return chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId);
    }

    private List<ChatMessage> convertAndSaveMessages(List<ChatMessageEntity> entities) {
        List<ChatMessage> messages = convertEntitiesToMessages(entities);
        saveMessagesToRedis(messages);
        return messages;
    }

    private List<ChatMessage> convertEntitiesToMessages(List<ChatMessageEntity> entities) {
        return entities.stream()
                .map(ChatMessageEntity::toChatMessage)
                .toList();
    }

    private void saveMessagesToRedis(List<ChatMessage> messages) {
        messages.forEach(redisChatRepository::saveMessage);
    }
}