package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatMessageEntity;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomEntity;
import com.dongsoop.dongsoop.chat.repository.ChatMessageJpaRepository;
import com.dongsoop.dongsoop.chat.repository.ChatRoomJpaRepository;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatBackupService {
    private final RedisChatRepository redisChatRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ChatMessageJpaRepository chatMessageJpaRepository;

    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 실행
    public void backupExpiringRooms() {
        List<ChatRoom> expiringSoonRooms = redisChatRepository.findRoomsOlderThan(25);

        for (ChatRoom room : expiringSoonRooms) {
            if (room.getParticipants().size() > 2) { // 그룹 채팅만 백업
                // 이미 저장되었는지 확인
                if (!chatRoomJpaRepository.existsById(room.getRoomId())) {
                    ChatRoomEntity entity = ChatRoomEntity.builder()
                            .roomId(room.getRoomId())
                            .isGroupChat(true)
                            .participants(room.getParticipants())
                            .createdAt(LocalDateTime.now().minusDays(25)) // 추정치
                            .lastActivityAt(LocalDateTime.now())
                            .build();

                    chatRoomJpaRepository.save(entity);

                    // 메시지도 저장
                    List<ChatMessage> messages = redisChatRepository.findMessagesByRoomId(room.getRoomId());
                    for (ChatMessage message : messages) {
                        ChatMessageEntity msgEntity = convertToEntity(message);
                        chatMessageJpaRepository.save(msgEntity);
                    }
                }
            }
        }
    }

    private ChatMessageEntity convertToEntity(ChatMessage message) {
        return ChatMessageEntity.builder()
                .messageId(message.getMessageId())
                .roomId(message.getRoomId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .type(message.getType())
                .build();
    }
}