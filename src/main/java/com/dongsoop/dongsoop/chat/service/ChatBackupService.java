package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomEntity;
import com.dongsoop.dongsoop.chat.repository.ChatRoomJpaRepository;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
public class ChatBackupService {
    private static final int BACKUP_DAYS_THRESHOLD = 25;

    private final RedisChatRepository redisChatRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;

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
        return participantCount >= 2;
    }

    private void backupRoomAndMessages(ChatRoom room) {
        if (room.isGroupChat()) {
            backupRoom(room);
        }
    }

    private void backupRoom(ChatRoom room) {
        ChatRoomEntity entity = room.toChatRoomEntity();
        chatRoomJpaRepository.save(entity);
    }
}