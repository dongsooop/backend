package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatBackupService;
import com.dongsoop.dongsoop.chat.service.ChatBackupWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatBackupServiceTest {

    @Mock
    private RedisChatRepository redisChatRepository;
    @Mock
    private ChatBackupWorker chatBackupWorker;

    @InjectMocks
    private ChatBackupService chatBackupService;

    @Test
    @DisplayName("performDailyMaintenance - 한 방 백업 실패해도 나머지 백업 계속")
    void performDailyMaintenance_continuesAfterFailure() {
        ChatRoom room1 = ChatRoom.builder()
                .roomId("room1")
                .lastActivityAt(LocalDateTime.now().minusDays(26))
                .participants(new HashSet<>())
                .isGroupChat(false)
                .build();
        ChatRoom room2 = ChatRoom.builder()
                .roomId("room2")
                .lastActivityAt(LocalDateTime.now().minusDays(26))
                .participants(new HashSet<>())
                .isGroupChat(false)
                .build();

        when(redisChatRepository.findRoomsWithLastActivityBefore(any()))
                .thenReturn(List.of(room1, room2))
                .thenReturn(List.of());

        when(chatBackupWorker.isRoomNotBackedUp("room1")).thenReturn(true);
        when(chatBackupWorker.isRoomNotBackedUp("room2")).thenReturn(true);
        doThrow(new RuntimeException("DB error")).when(chatBackupWorker).backupRoom(room1);
        doNothing().when(chatBackupWorker).backupRoom(room2);

        chatBackupService.performDailyMaintenance();

        verify(chatBackupWorker).backupRoom(room1);
        verify(chatBackupWorker).backupRoom(room2);
    }

    @Test
    @DisplayName("performDailyMaintenance - 이미 백업된 방은 스킵")
    void performDailyMaintenance_skipsAlreadyBackedUp() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .lastActivityAt(LocalDateTime.now().minusDays(26))
                .participants(new HashSet<>())
                .build();

        when(redisChatRepository.findRoomsWithLastActivityBefore(any()))
                .thenReturn(List.of(room))
                .thenReturn(List.of());

        when(chatBackupWorker.isRoomNotBackedUp("room1")).thenReturn(false);

        chatBackupService.performDailyMaintenance();

        verify(chatBackupWorker, never()).backupRoom(any());
    }

    @Test
    @DisplayName("performDailyMaintenance - 만료 방 삭제 실패해도 계속")
    void performDailyMaintenance_deletesContinuesAfterFailure() {
        ChatRoom room1 = ChatRoom.builder()
                .roomId("room1")
                .lastActivityAt(LocalDateTime.now().minusDays(31))
                .participants(new HashSet<>())
                .isGroupChat(false)
                .build();
        ChatRoom room2 = ChatRoom.builder()
                .roomId("room2")
                .lastActivityAt(LocalDateTime.now().minusDays(31))
                .participants(new HashSet<>())
                .isGroupChat(false)
                .build();

        when(redisChatRepository.findRoomsWithLastActivityBefore(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(room1, room2));

        doThrow(new RuntimeException("Redis error")).when(redisChatRepository).deleteRoom("room1");
        doNothing().when(redisChatRepository).deleteRoom("room2");

        chatBackupService.performDailyMaintenance();

        verify(redisChatRepository).deleteRoom("room1");
        verify(redisChatRepository).deleteRoom("room2");
    }
}
