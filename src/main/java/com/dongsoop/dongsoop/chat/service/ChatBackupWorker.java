package com.dongsoop.dongsoop.chat.service;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomEntity;
import com.dongsoop.dongsoop.chat.repository.ChatRoomJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBackupWorker {
    private final ChatRoomJpaRepository chatRoomJpaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void backupRoom(ChatRoom room) {
        ChatRoomEntity entity = room.toChatRoomEntity();
        chatRoomJpaRepository.save(entity);
        logBackupSuccess(entity.getRoomId(), entity.isGroupChat());
    }

    public boolean isRoomNotBackedUp(String roomId) {
        return !chatRoomJpaRepository.existsById(roomId);
    }

    private void logBackupSuccess(String roomId, boolean isGroupChat) {
        String roomType = isGroupChat ? "그룹" : "1:1";
        log.info("{} 채팅방 백업 완료: {}", roomType, roomId);
    }
}
