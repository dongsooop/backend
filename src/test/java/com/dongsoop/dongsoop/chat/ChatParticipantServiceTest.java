package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.GroupChatOnlyException;
import com.dongsoop.dongsoop.chat.exception.ManagerKickAttemptException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private ChatRoom groupRoom(Set<Long> participants) {
        return ChatRoom.builder()
                .roomId("room1")
                .isGroupChat(true)
                .managerId(1L)
                .participants(new HashSet<>(participants))
                .kickedUsers(new HashSet<>())
                .participantJoinTimes(new java.util.HashMap<>())
                .build();
    }

    @Test
    @DisplayName("관리자 추방은 방장 권한 없이 대상을 내보내고 재입장을 막는다")
    void kickUserByAdmin_KicksParticipant() {
        ChatRoom room = groupRoom(Set.of(1L, 2L));
        when(chatRoomService.getChatRoomById("room1")).thenReturn(room);

        chatParticipantService.kickUserByAdmin("room1", 2L);

        assertThat(room.getParticipants()).doesNotContain(2L);
        assertThat(room.isKicked(2L)).isTrue();
        verify(chatMessageService).createAndSaveSystemMessage("room1", 2L, MessageType.LEAVE);
        verify(chatRoomService).saveRoom(room);
    }

    @Test
    @DisplayName("이미 나간 대상은 시스템 메시지 없이 재입장만 막는다")
    void kickUserByAdmin_AlreadyLeft_BlocksRejoinOnly() {
        ChatRoom room = groupRoom(Set.of(1L));
        when(chatRoomService.getChatRoomById("room1")).thenReturn(room);

        chatParticipantService.kickUserByAdmin("room1", 2L);

        assertThat(room.isKicked(2L)).isTrue();
        verify(chatMessageService, never()).createAndSaveSystemMessage("room1", 2L, MessageType.LEAVE);
    }

    @Test
    @DisplayName("1:1 방에서는 관리자 추방을 거절한다")
    void kickUserByAdmin_OneToOne_Throws() {
        ChatRoom room = ChatRoom.create(1L, 2L, "1:1");
        when(chatRoomService.getChatRoomById("room1")).thenReturn(room);

        assertThatThrownBy(() -> chatParticipantService.kickUserByAdmin("room1", 2L))
                .isInstanceOf(GroupChatOnlyException.class);
        verify(chatRoomService, never()).saveRoom(any());
        verify(chatMessageService, never()).createAndSaveSystemMessage(any(), any(), any());
    }

    @Test
    @DisplayName("방장은 관리자 추방 대상이 될 수 없다")
    void kickUserByAdmin_Manager_Throws() {
        ChatRoom room = groupRoom(Set.of(1L, 2L));
        when(chatRoomService.getChatRoomById("room1")).thenReturn(room);
        doThrow(new ManagerKickAttemptException()).when(chatValidator).validateNotKickingManager(room, 1L);

        assertThatThrownBy(() -> chatParticipantService.kickUserByAdmin("room1", 1L))
                .isInstanceOf(ManagerKickAttemptException.class);
        verify(chatRoomService, never()).saveRoom(any());
        verify(chatMessageService, never()).createAndSaveSystemMessage(any(), any(), any());
    }
}
