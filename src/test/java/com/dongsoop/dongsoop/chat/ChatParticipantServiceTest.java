package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.service.ChatMessageService;
import com.dongsoop.dongsoop.chat.service.ChatParticipantService;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.chat.service.ReadStatusService;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatParticipantServiceTest {

    @Mock
    private ChatValidator chatValidator;
    @Mock
    private ReadStatusService readStatusService;
    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private ChatMessageService chatMessageService;

    @InjectMocks
    private ChatParticipantService chatParticipantService;

    @Test
    @DisplayName("inviteUserToGroupChat - 주입된 서비스로 동작")
    void inviteUserToGroupChat_usesInjectedServices() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .isGroupChat(true)
                .managerId(1L)
                .participants(new HashSet<>(Set.of(1L)))
                .kickedUsers(new HashSet<>())
                .participantJoinTimes(new java.util.HashMap<>())
                .build();

        ChatMessage enterMsg = ChatMessage.builder().messageId("m1").build();

        when(chatRoomService.getChatRoomById("room1")).thenReturn(room);
        when(chatRoomService.saveRoom(any())).thenReturn(room);
        when(chatMessageService.createAndSaveSystemMessage("room1", 2L, MessageType.ENTER)).thenReturn(enterMsg);

        ChatMessage result = chatParticipantService.inviteUserToGroupChat("room1", 1L, 2L);

        assertThat(result).isEqualTo(enterMsg);
        verify(chatRoomService).saveRoom(any());
        verify(readStatusService).initializeUserReadStatus(eq(2L), eq("room1"), any());
    }

    @Test
    @DisplayName("kickUserFromRoom - 주입된 서비스로 동작")
    void kickUserFromRoom_usesInjectedServices() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .isGroupChat(true)
                .managerId(1L)
                .participants(new HashSet<>(Set.of(1L, 2L)))
                .kickedUsers(new HashSet<>())
                .participantJoinTimes(new java.util.HashMap<>())
                .build();

        when(chatRoomService.getChatRoomById("room1")).thenReturn(room);
        when(chatRoomService.saveRoom(any())).thenReturn(room);

        chatParticipantService.kickUserFromRoom("room1", 1L, 2L);

        verify(chatMessageService).createAndSaveSystemMessage("room1", 2L, MessageType.LEAVE);
        verify(chatRoomService).saveRoom(any());
    }

    @Test
    @DisplayName("leaveChatRoom - 일반 채팅방 나가기")
    void leaveChatRoom_normalRoom() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .title("일반 채팅")
                .isGroupChat(true)
                .managerId(1L)
                .participants(new HashSet<>(Set.of(1L, 2L)))
                .kickedUsers(new HashSet<>())
                .participantJoinTimes(new java.util.HashMap<>())
                .build();

        when(chatRoomService.getChatRoomById("room1")).thenReturn(room);
        when(chatRoomService.saveRoom(any())).thenReturn(room);

        chatParticipantService.leaveChatRoom("room1", 2L);

        verify(chatMessageService).createAndSaveSystemMessage("room1", 2L, MessageType.LEAVE);
        verify(chatRoomService).saveRoom(any());
    }

    @Test
    @DisplayName("leaveChatRoom - 문의 채팅방은 별도 처리")
    void leaveChatRoom_contactRoom() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .title("[문의] 테스트")
                .participants(new HashSet<>(Set.of(1L, 2L)))
                .build();

        when(chatRoomService.getChatRoomById("room1")).thenReturn(room);

        chatParticipantService.leaveChatRoom("room1", 2L);

        verify(chatRoomService).handleContactRoomLeave("room1", 2L);
        verify(chatMessageService).createAndSaveSystemMessage("room1", 2L, MessageType.LEAVE);
    }

    @Test
    @DisplayName("determineUserJoinTime - 기존 joinTime이 있으면 반환")
    void determineUserJoinTime_existingJoinTime() {
        LocalDateTime joinTime = LocalDateTime.now().minusDays(1);
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participantJoinTimes(new java.util.HashMap<>(java.util.Map.of(1L, joinTime)))
                .build();

        LocalDateTime result = chatParticipantService.determineUserJoinTime(room, 1L);

        assertThat(result).isEqualTo(joinTime);
        verify(chatRoomService, never()).saveRoom(any());
    }

    @Test
    @DisplayName("determineUserJoinTime - joinTime 없으면 새 참가자로 추가")
    void determineUserJoinTime_noJoinTime_addsParticipant() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participants(new HashSet<>(Set.of(1L)))
                .participantJoinTimes(new java.util.HashMap<>())
                .build();

        when(chatRoomService.saveRoom(any())).thenReturn(room);

        LocalDateTime result = chatParticipantService.determineUserJoinTime(room, 2L);

        assertThat(result).isNotNull();
        verify(chatRoomService).saveRoom(any());
    }
}
