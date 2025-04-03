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
import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class ChatBackupService {
    private static final int BACKUP_DAYS_THRESHOLD = 25;

    private final RedisChatRepository redisChatRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final ChatMessageJpaRepository chatMessageJpaRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void backupExpiringRooms() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(BACKUP_DAYS_THRESHOLD);
        List<ChatRoom> expiringSoonRooms = redisChatRepository.findRoomsCreatedBefore(cutoffTime);

        Predicate<ChatRoom> isGroupChatNotBackedUp = room ->
                isGroupRoom(room.getParticipants().size()) &&
                        !chatRoomJpaRepository.existsById(room.getRoomId());

        expiringSoonRooms.stream()
                .filter(isGroupChatNotBackedUp)
                .forEach(this::backupRoomAndMessages);
    }

    private boolean isGroupRoom(int participantCount) {
        return participantCount > 2;
    }

    private void backupRoomAndMessages(ChatRoom room) {
        backupRoom(room);
        backupMessages(room.getRoomId());
    }

    private void backupRoom(ChatRoom room) {
        ChatRoomEntity entity = createRoomEntity(room);
        chatRoomJpaRepository.save(entity);
    }

    private ChatRoomEntity createRoomEntity(ChatRoom room) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = resolveCreatedAt(room.getCreatedAt(), now);

        return ChatRoomEntity.builder()
                .roomId(room.getRoomId())
                .isGroupChat(true)
                .participants(room.getParticipants())
                .createdAt(createdAt)
                .lastActivityAt(now)
                .build();
    }

    private LocalDateTime resolveCreatedAt(LocalDateTime timestamp, LocalDateTime defaultTime) {
        return Optional.ofNullable(timestamp)
                .orElseGet(() -> defaultTime.minusDays(BACKUP_DAYS_THRESHOLD));
    }

    private void backupMessages(String roomId) {
        redisChatRepository.findMessagesByRoomId(roomId).stream()
                .map(this::convertToEntity)
                .forEach(chatMessageJpaRepository::save);
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