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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatSyncService {
    private final RedisChatRepository redisChatRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ChatMessageJpaRepository chatMessageJpaRepository;

    // Redis에서 만료된 그룹 채팅방 복원
    public ChatRoom restoreGroupChatRoom(String roomId) {
        Optional<ChatRoomEntity> roomEntityOpt = chatRoomJpaRepository.findById(roomId);
        if (roomEntityOpt.isPresent()) {
            ChatRoomEntity entity = roomEntityOpt.get();

            if (entity.isGroupChat()) {
                ChatRoom room = new ChatRoom();
                room.setRoomId(entity.getRoomId());
                room.setParticipants(entity.getParticipants());

                // Redis에 저장
                redisChatRepository.saveRoom(room);

                // 최근 메시지도 Redis로 복원
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
}