package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.repository.ChatRoomJpaRepository;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBackupService {
    private static final int BACKUP_DAYS_THRESHOLD = 25;
    private static final int DELETE_DAYS_THRESHOLD = 30;

    private final RedisChatRepository redisChatRepository;
    private final ChatRoomJpaRepository chatRoomJpaRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void performDailyMaintenance() {
        performRoomBackup();
        performExpiredRoomDeletion();
    }

    private void performRoomBackup() {
        LocalDateTime backupCutoffTime = calculateBackupCutoffTime();
        List<ChatRoom> roomsNeedingBackup = findRoomsNeedingBackup(backupCutoffTime);
        processBackupCandidates(roomsNeedingBackup);
    }

    private void performExpiredRoomDeletion() {
        LocalDateTime deletionCutoffTime = calculateDeletionCutoffTime();
        List<ChatRoom> expiredRooms = findExpiredRooms(deletionCutoffTime);
        processExpiredRoomDeletion(expiredRooms);
    }

    private LocalDateTime calculateBackupCutoffTime() {
        return LocalDateTime.now().minusDays(BACKUP_DAYS_THRESHOLD);
    }

    private LocalDateTime calculateDeletionCutoffTime() {
        return LocalDateTime.now().minusDays(DELETE_DAYS_THRESHOLD);
    }

    private List<ChatRoom> findRoomsNeedingBackup(LocalDateTime cutoffTime) {
        return redisChatRepository.findRoomsWithLastActivityBefore(cutoffTime);
    }

    private List<ChatRoom> findExpiredRooms(LocalDateTime cutoffTime) {
        return redisChatRepository.findRoomsWithLastActivityBefore(cutoffTime);
    }

    private void processBackupCandidates(List<ChatRoom> rooms) {
        rooms.stream()
                .filter(createNotBackedUpFilter())
                .forEach(this::backupRoom);
    }

    private void processExpiredRoomDeletion(List<ChatRoom> rooms) {
        rooms.forEach(this::deleteExpiredRoom);
    }

    private Predicate<ChatRoom> createNotBackedUpFilter() {
        return room -> isRoomNotBackedUp(room.getRoomId());
    }

    private boolean isRoomNotBackedUp(String roomId) {
        return !chatRoomJpaRepository.existsById(roomId);
    }

    private void backupRoom(ChatRoom room) {
        Optional.of(room)
                .map(ChatRoom::toChatRoomEntity)
                .ifPresent(this::saveRoomEntity);
    }

    private void saveRoomEntity(com.dongsoop.dongsoop.chat.entity.ChatRoomEntity entity) {
        chatRoomJpaRepository.save(entity);
        logBackupSuccess(entity.getRoomId(), entity.isGroupChat());
    }

    private void logBackupSuccess(String roomId, boolean isGroupChat) {
        String roomType = determineRoomType(isGroupChat);
        log.info("{} 채팅방 백업 완료: {}", roomType, roomId);
    }

    private String determineRoomType(boolean isGroupChat) {
        if (isGroupChat) {
            return "그룹";
        }
        return "1:1";
    }

    private void deleteExpiredRoom(ChatRoom room) {
        deleteRoomFromRedis(room.getRoomId());
        logRoomDeletionSuccess(room.getRoomId(), room.isGroupChat());
    }

    private void deleteRoomFromRedis(String roomId) {
        redisChatRepository.deleteRoom(roomId);
    }

    private void logRoomDeletionSuccess(String roomId, boolean isGroupChat) {
        String roomType = determineRoomType(isGroupChat);
        log.info("만료된 {} 채팅방 삭제 완료: {}", roomType, roomId);
    }
}