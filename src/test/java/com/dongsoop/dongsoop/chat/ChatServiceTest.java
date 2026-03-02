package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.dto.ChatRoomListResponse;
import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.notification.ChatNotification;
import com.dongsoop.dongsoop.chat.service.*;
import com.dongsoop.dongsoop.chat.session.WebSocketSessionManager;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberblock.constant.BlockStatus;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRoomService chatRoomService;
    @Mock
    private ChatMessageService chatMessageService;
    @Mock
    private ChatParticipantService chatParticipantService;
    @Mock
    private ReadStatusService readStatusService;
    @Mock
    private ChatValidator chatValidator;
    @Mock
    private MemberBlockRepository memberBlockRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ChatNotification chatNotification;
    @Mock
    private MemberService memberService;
    @Mock
    private WebSocketSessionManager sessionManager;

    @InjectMocks
    private ChatService chatService;

    @Test
    @DisplayName("getBlockStatus - 1:1 채팅에서 참여자가 자기밖에 없을 때 NONE 반환")
    void getBlockStatus_singleParticipant_returnsNone() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .isGroupChat(false)
                .participants(new HashSet<>(Set.of(1L)))
                .build();
        when(chatRoomService.getChatRoomById("room1")).thenReturn(room);

        BlockStatus result = chatService.getBlockStatus("room1", 1L);

        assertThat(result).isEqualTo(BlockStatus.NONE);
    }

    @Test
    @DisplayName("getBlockStatus - 그룹 채팅은 항상 NONE")
    void getBlockStatus_groupChat_returnsNone() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .isGroupChat(true)
                .participants(new HashSet<>(Set.of(1L, 2L)))
                .build();
        when(chatRoomService.getChatRoomById("room1")).thenReturn(room);

        BlockStatus result = chatService.getBlockStatus("room1", 1L);

        assertThat(result).isEqualTo(BlockStatus.NONE);
    }

    @Test
    @DisplayName("processWebSocketMessage - 온라인 사용자는 FCM 발송에서 제외")
    void processWebSocketMessage_skipsFcmForOnlineUsers() {
        ChatMessage message = ChatMessage.builder()
                .roomId("room1")
                .senderId(1L)
                .content("hello")
                .timestamp(LocalDateTime.now())
                .type(MessageType.CHAT)
                .build();

        ChatMessage processed = ChatMessage.builder()
                .messageId("msg1")
                .roomId("room1")
                .senderId(1L)
                .content("hello")
                .timestamp(LocalDateTime.now())
                .type(MessageType.CHAT)
                .build();

        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participants(new HashSet<>(Set.of(1L, 2L, 3L)))
                .build();

        when(chatMessageService.processWebSocketMessage(any(), eq(1L), eq("room1"))).thenReturn(processed);
        when(chatRoomService.updateRoomActivity("room1")).thenReturn(room);
        when(sessionManager.isUserOnline(2L)).thenReturn(true);
        when(sessionManager.isUserOnline(3L)).thenReturn(false);
        when(memberService.getNicknameById(1L)).thenReturn("sender");
        when(readStatusService.getLastReadTimestampsBatchForUsers(anyList(), eq("room1")))
                .thenReturn(Map.of(1L, LocalDateTime.now(), 2L, LocalDateTime.now(), 3L, LocalDateTime.now()));
        when(chatMessageService.loadMessagesAfterJoinTime(anyString(), any())).thenReturn(List.of());

        chatService.processWebSocketMessage(message, 1L, "room1");

        verify(chatNotification).send(argThat(set -> set.contains(3L) && !set.contains(2L)),
                eq("room1"), eq("sender"), eq("hello"));
    }

    @Test
    @DisplayName("processWebSocketMessage - 모든 수신자가 온라인이면 FCM 미발송")
    void processWebSocketMessage_allOnline_noFcm() {
        ChatMessage message = ChatMessage.builder()
                .roomId("room1").senderId(1L).content("hello")
                .timestamp(LocalDateTime.now()).type(MessageType.CHAT).build();
        ChatMessage processed = ChatMessage.builder()
                .messageId("msg1").roomId("room1").senderId(1L).content("hello")
                .timestamp(LocalDateTime.now()).type(MessageType.CHAT).build();
        ChatRoom room = ChatRoom.builder()
                .roomId("room1").participants(new HashSet<>(Set.of(1L, 2L))).build();

        when(chatMessageService.processWebSocketMessage(any(), eq(1L), eq("room1"))).thenReturn(processed);
        when(chatRoomService.updateRoomActivity("room1")).thenReturn(room);
        when(sessionManager.isUserOnline(2L)).thenReturn(true);
        when(readStatusService.getLastReadTimestampsBatchForUsers(anyList(), eq("room1")))
                .thenReturn(Map.of(1L, LocalDateTime.now(), 2L, LocalDateTime.now()));
        when(chatMessageService.loadMessagesAfterJoinTime(anyString(), any())).thenReturn(List.of());

        chatService.processWebSocketMessage(message, 1L, "room1");

        verify(chatNotification, never()).send(any(), any(), any(), any());
    }

    @Test
    @DisplayName("sendGlobalRoomUpdate - 배치 조회 1회로 처리")
    void sendGlobalRoomUpdate_usesBatchReadTimestamps() {
        ChatMessage message = ChatMessage.builder()
                .roomId("room1").senderId(1L).content("hello")
                .timestamp(LocalDateTime.now()).type(MessageType.CHAT).build();
        ChatMessage processed = ChatMessage.builder()
                .messageId("msg1").roomId("room1").senderId(1L).content("hello")
                .timestamp(LocalDateTime.now()).type(MessageType.CHAT).build();
        ChatRoom room = ChatRoom.builder()
                .roomId("room1").participants(new HashSet<>(Set.of(1L, 2L, 3L))).build();

        when(chatMessageService.processWebSocketMessage(any(), eq(1L), eq("room1"))).thenReturn(processed);
        when(chatRoomService.updateRoomActivity("room1")).thenReturn(room);
        when(sessionManager.isUserOnline(anyLong())).thenReturn(true);
        when(readStatusService.getLastReadTimestampsBatchForUsers(anyList(), eq("room1")))
                .thenReturn(Map.of(1L, LocalDateTime.now(), 2L, LocalDateTime.now(), 3L, LocalDateTime.now()));
        when(chatMessageService.loadMessagesAfterJoinTime(anyString(), any())).thenReturn(List.of());

        chatService.processWebSocketMessage(message, 1L, "room1");

        verify(readStatusService, times(1)).getLastReadTimestampsBatchForUsers(anyList(), eq("room1"));
        verify(messagingTemplate, times(3)).convertAndSend(startsWith("/topic/user/"), any(Object.class));
    }

    @Test
    @DisplayName("buildRoomListResponses - 배치 메서드를 활용하여 응답 생성")
    void buildRoomListResponses_usesBatchMethods() {
        ChatRoom room1 = ChatRoom.builder()
                .roomId("r1").title("방1").participants(new HashSet<>(Set.of(1L, 2L)))
                .lastActivityAt(LocalDateTime.now()).isGroupChat(false).build();
        ChatRoom room2 = ChatRoom.builder()
                .roomId("r2").title("[문의] 제목").participants(new HashSet<>(Set.of(1L, 3L)))
                .lastActivityAt(LocalDateTime.now()).isGroupChat(false).build();

        when(readStatusService.getLastReadTimestampsBatch(eq(1L), anyList()))
                .thenReturn(Map.of("r1", LocalDateTime.now(), "r2", LocalDateTime.now()));
        when(chatMessageService.getLastMessageTextsBatch(anyList()))
                .thenReturn(Map.of("r1", "hello", "r2", "문의드립니다"));
        when(chatMessageService.loadMessagesAfterJoinTime(anyString(), any())).thenReturn(List.of());

        List<ChatRoomListResponse> result = chatService.buildRoomListResponses(List.of(room1, room2), 1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getLastMessage()).isEqualTo("hello");
        assertThat(result.get(1).getRoomType()).isEqualTo("contact");
        verify(readStatusService).getLastReadTimestampsBatch(eq(1L), anyList());
        verify(chatMessageService).getLastMessageTextsBatch(anyList());
    }

    @Test
    @DisplayName("kickUserFromRoom - 서비스 파라미터 없이 호출")
    void kickUserFromRoom_callsWithoutServiceParams() {
        ChatRoom room = ChatRoom.builder().roomId("room1").build();
        when(chatParticipantService.kickUserFromRoom("room1", 1L, 2L)).thenReturn(room);

        ChatRoom result = chatService.kickUserFromRoom("room1", 1L, 2L);

        assertThat(result).isEqualTo(room);
        verify(chatParticipantService).kickUserFromRoom("room1", 1L, 2L);
    }
}
