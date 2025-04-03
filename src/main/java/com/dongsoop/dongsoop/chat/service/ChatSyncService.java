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
        Optional<ChatRoomEntity> roomEntityOpt = chatRoomJpaRepository.findById(roomId);
        if (roomEntityOpt.isPresent()) {
            ChatRoomEntity entity = roomEntityOpt.get();

            if (entity.isGroupChat()) {
                ChatRoom room = new ChatRoom();
                room.setRoomId(entity.getRoomId());
                room.setParticipants(entity.getParticipants());

                redisChatRepository.saveRoom(room);

                List<ChatMessageEntity> messageEntities =
                        chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId);

                for (ChatMessageEntity msgEntity : messageEntities) {
                    ChatMessage message = convertToMessage(msgEntity);
                    redisChatRepository.saveMessage(message);
                }

                return room;
            }
        }
        return null;
    }

    private ChatMessage convertToMessage(ChatMessageEntity entity) {
        return ChatMessage.builder()
                .messageId(entity.getMessageId())
                .roomId(entity.getRoomId())
                .senderId(entity.getSenderId())
                .content(entity.getContent())
                .timestamp(entity.getTimestamp())
                .type(entity.getType())
                .build();
    }

    public List<ChatMessage> restoreMessagesFromDatabase(String roomId) {
        List<ChatMessageEntity> messageEntities = chatMessageJpaRepository.findByRoomIdOrderByTimestampAsc(roomId);
        if (messageEntities.isEmpty()) {
            return Collections.emptyList();
        }

        List<ChatMessage> messages = messageEntities.stream()
                .map(this::convertToMessage)
                .collect(Collectors.toList());

        for (ChatMessage message : messages) {
            redisChatRepository.saveMessage(message);
        }

        return messages;
    }
}