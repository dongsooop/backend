package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBackupService {
    private static final int BACKUP_DAYS_THRESHOLD = 25;
    private static final int DELETE_DAYS_THRESHOLD = 30;

    private final RedisChatRepository redisChatRepository;
    private final ChatBackupWorker chatBackupWorker;

    @Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
    public void performDailyMaintenance() {
        performRoomBackup();
        performExpiredRoomDeletion();
    }

    private void performRoomBackup() {
        LocalDateTime backupCutoffTime = LocalDateTime.now().minusDays(BACKUP_DAYS_THRESHOLD);
        List<ChatRoom> roomsNeedingBackup = redisChatRepository.findRoomsWithLastActivityBefore(backupCutoffTime);

        for (ChatRoom room : roomsNeedingBackup) {
            if (!chatBackupWorker.isRoomNotBackedUp(room.getRoomId())) {
                continue;
            }
            try {
                chatBackupWorker.backupRoom(room);
            } catch (Exception e) {
                log.error("채팅방 백업 실패: roomId={}", room.getRoomId(), e);
            }
        }
    }

    private void performExpiredRoomDeletion() {
        LocalDateTime deletionCutoffTime = LocalDateTime.now().minusDays(DELETE_DAYS_THRESHOLD);
        List<ChatRoom> expiredRooms = redisChatRepository.findRoomsWithLastActivityBefore(deletionCutoffTime);

        for (ChatRoom room : expiredRooms) {
            try {
                redisChatRepository.deleteRoom(room.getRoomId());
                String roomType = room.isGroupChat() ? "그룹" : "1:1";
                log.info("만료된 {} 채팅방 삭제 완료: {}", roomType, room.getRoomId());
            } catch (Exception e) {
                log.error("만료된 채팅방 삭제 실패: roomId={}", room.getRoomId(), e);
            }
        }
    }
}
