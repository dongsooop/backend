package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
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
    private static final int MIN_GROUP_SIZE = 2;

    private final RedisChatRepository redisChatRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;

    @Scheduled(cron = "0 0 0 * * *")
    public void backupExpiringRooms() {
        LocalDateTime cutoffTime = calculateBackupCutoffTime();
        List<ChatRoom> expiringSoonRooms = findExpiringSoonRooms(cutoffTime);

        processBackupCandidates(expiringSoonRooms);
    }

    private LocalDateTime calculateBackupCutoffTime() {
        return LocalDateTime.now().minusDays(BACKUP_DAYS_THRESHOLD);
    }

    private List<ChatRoom> findExpiringSoonRooms(LocalDateTime cutoffTime) {
        return redisChatRepository.findRoomsCreatedBefore(cutoffTime);
    }

    private void processBackupCandidates(List<ChatRoom> rooms) {
        rooms.stream()
                .filter(createGroupRoomFilter())
                .filter(createNotBackedUpFilter())
                .forEach(this::backupGroupRoom);
    }

    private Predicate<ChatRoom> createGroupRoomFilter() {
        return room -> room.getParticipants().size() >= MIN_GROUP_SIZE;
    }

    private Predicate<ChatRoom> createNotBackedUpFilter() {
        return room -> !chatRoomJpaRepository.existsById(room.getRoomId());
    }

    private void backupGroupRoom(ChatRoom room) {
        Optional.of(room)
                .filter(ChatRoom::isGroupChat)
                .map(ChatRoom::toChatRoomEntity)
                .ifPresent(chatRoomJpaRepository::save);
    }
}