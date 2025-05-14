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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSyncService {
    private final RedisChatRepository redisChatRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ChatMessageJpaRepository chatMessageJpaRepository;

    public ChatRoom restoreGroupChatRoom(String roomId) {
        return chatRoomJpaRepository.findById(roomId)
                .filter(ChatRoomEntity::isGroupChat)
                .map(this::convertAndSaveRoom)
                .orElse(null);
    }

    private ChatRoom convertAndSaveRoom(ChatRoomEntity entity) {
        ChatRoom room = entity.toChatRoom();
        redisChatRepository.saveRoom(room);
        restoreAndSaveMessages(entity.getRoomId());
        return room;
    }

    private void restoreAndSaveMessages(String roomId) {
        chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId)
                .stream()
                .map(ChatMessageEntity::toChatMessage)
                .forEach(redisChatRepository::saveMessage);
    }

    public List<ChatMessage> restoreMessagesFromDatabase(String roomId) {
        List<ChatMessageEntity> messageEntities =
                chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId);

        return Optional.of(messageEntities)
                .filter(list -> !list.isEmpty())
                .map(this::mapAndSaveMessages)
                .orElse(Collections.emptyList());
    }

    private List<ChatMessage> mapAndSaveMessages(List<ChatMessageEntity> entities) {
        List<ChatMessage> messages = entities.stream()
                .map(ChatMessageEntity::toChatMessage)
                .collect(Collectors.toList());

        messages.forEach(redisChatRepository::saveMessage);
        return messages;
    }
}