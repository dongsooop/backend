package com.dongsoop.dongsoop.chat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.chat.dto.*;
import com.dongsoop.dongsoop.chat.entity.*;
import com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException;
import com.dongsoop.dongsoop.chat.notification.ChatNotification;
import com.dongsoop.dongsoop.chat.service.*;
import com.dongsoop.dongsoop.chat.session.WebSocketSessionManager;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberblock.repository.MemberBlockRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatReadDeliveryContractTest {
  private static final LocalDateTime TIME = LocalDateTime.of(2026, 10, 14, 12, 0);
  @Mock ChatRoomService rooms;
  @Mock ChatMessageService messages;
  @Mock ChatParticipantService participants;
  @Mock ReadStatusService reads;
  @Mock ChatValidator validator;
  @Mock MemberBlockRepository blocks;
  @Mock SimpMessagingTemplate socket;
  @Mock ChatNotification push;
  @Mock MemberService members;
  @Mock WebSocketSessionManager sessions;
  @InjectMocks ChatService service;

  @Test
  void initializationReturnsOnlyMessagesAfterJoinAndInitializesReadTime() {
    ChatRoom room = ChatRoom.create(7L, 8L, "room");
    ChatMessage message = message("m1", 8L, TIME);
    when(rooms.getChatRoomById("room")).thenReturn(room);
    when(participants.determineUserJoinTime(room, 7L)).thenReturn(TIME);
    when(messages.loadMessagesAfterJoinTime("room", TIME)).thenReturn(List.of(message));
    ChatRoomInitResponse response = service.initializeChatRoomForFirstTime("room", 7L);
    assertThat(response.getRoom()).isSameAs(room);
    assertThat(response.getMessages()).containsExactly(message);
    assertThat(response.getTotalMessageCount()).isEqualTo(1);
    assertThat(response.getUserJoinTime()).isEqualTo(TIME);
    verify(reads).initializeUserReadStatus(7L, "room", TIME);
  }

  @Test
  void unauthorizedReadCannotLoadOrChangeMessages() {
    doThrow(new UnauthorizedChatAccessException()).when(validator).validateUserForRoom("room", 7L);
    assertThatThrownBy(() -> service.initializeChatRoomForFirstTime("room", 7L))
        .isInstanceOf(UnauthorizedChatAccessException.class);
    ReadStatusUpdateRequest request = new ReadStatusUpdateRequest(TIME, null);
    assertThatThrownBy(() -> service.updateReadStatus("room", 7L, request))
        .isInstanceOf(UnauthorizedChatAccessException.class);
    assertThatThrownBy(() -> service.getMessagesAfter("room", 7L, "m1"))
        .isInstanceOf(UnauthorizedChatAccessException.class);
    verifyNoInteractions(rooms, messages, reads);
  }

  @Test
  void explicitReadTimeTakesPrecedenceOverMessageId() {
    service.updateReadStatus("room", 7L, new ReadStatusUpdateRequest(TIME, "m1"));
    verify(reads).updateLastReadTimestamp(7L, "room", TIME);
    verifyNoInteractions(messages);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void readByMessageIdUsesStoredTimestampAndMissingMessageDoesNotAdvance(boolean found) {
    List<ChatMessage> history = List.of(message("m1", 8L, TIME));
    when(messages.getAllMessages("room")).thenReturn(history);
    if (found) {
      when(messages.findMessageById(history, "m1")).thenReturn(history.get(0));
    }
    service.updateReadStatus("room", 7L, new ReadStatusUpdateRequest(null, "m1"));
    if (found) {
      verify(reads).updateLastReadTimestamp(7L, "room", TIME);
      return;
    }
    verifyNoInteractions(reads);
  }

  @Test
  void emptyReadRequestUsesCurrentTimeAndMarkAllNotifiesOtherParticipants() {
    LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    service.updateReadStatus("room", 7L, new ReadStatusUpdateRequest(null, null));
    ArgumentCaptor<LocalDateTime> updated = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(reads).updateLastReadTimestamp(eq(7L), eq("room"), updated.capture());
    assertThat(updated.getValue()).isBetween(before, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    ChatRoom room = ChatRoom.createWithParticipantsAndTitle(Set.of(8L, 9L), 7L, "group");
    when(rooms.getChatRoomById("room")).thenReturn(room);
    service.markAllMessagesAsRead("room", 7L);
    ArgumentCaptor<Object> notifications = ArgumentCaptor.forClass(Object.class);
    verify(socket).convertAndSend(eq("/topic/user/8"), notifications.capture());
    verify(socket).convertAndSend(eq("/topic/user/9"), notifications.capture());
    assertThat(notifications.getAllValues())
        .allSatisfy(
            value -> {
              assertThat(value).isInstanceOf(Map.class);
              assertThat(value)
                  .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                  .containsEntry("roomId", "room")
                  .containsEntry("readerId", 7L)
                  .containsEntry("type", ChatNotificationType.READ_STATUS_UPDATE);
            });
    verify(socket, never()).convertAndSend(eq("/topic/user/7"), any(Object.class));
  }

  @Test
  void unreadCountIsZeroWithoutReadTimestampAndUsesReadBoundaryOtherwise() {
    assertThat(service.getUnreadMessageCount("room", 7L)).isZero();
    verifyNoInteractions(messages);
    when(reads.getLastReadTimestamp(7L, "room")).thenReturn(TIME);
    List<ChatMessage> history = List.of(message("m1", 8L, TIME.plusMinutes(1)));
    when(messages.loadMessagesAfterJoinTime("room", TIME)).thenReturn(history);
    when(messages.countUnreadMessages(history, 7L)).thenReturn(1);
    assertThat(service.getUnreadMessageCount("room", 7L)).isEqualTo(1);
  }

  @Test
  void roomUpdatesCountOnlyOtherSendersAfterEachReadBoundary() {
    ChatRoom room = ChatRoom.createWithParticipantsAndTitle(Set.of(8L, 9L), 7L, "group");
    ChatMessage input = message("new", 7L, TIME.plusMinutes(3));
    when(messages.processWebSocketMessage(input, 7L, "room")).thenReturn(input);
    when(rooms.updateRoomActivity("room")).thenReturn(room);
    when(sessions.isUserOnline(anyLong())).thenReturn(true);
    Map<Long, LocalDateTime> timestamps = new HashMap<>();
    timestamps.put(7L, TIME);
    timestamps.put(8L, TIME.plusMinutes(1));
    timestamps.put(9L, null);
    when(reads.getLastReadTimestampsBatchForUsers(anyList(), eq("room"))).thenReturn(timestamps);
    when(messages.loadMessagesAfterJoinTime("room", TIME))
        .thenReturn(
            List.of(
                message("equal", 8L, TIME),
                message("first", 8L, TIME.plusMinutes(1)),
                message("second", 7L, TIME.plusMinutes(2)),
                message("third", 9L, TIME.plusMinutes(3)),
                message("no-time", 8L, null)));
    assertThat(service.processWebSocketMessage(input, 7L, "room")).isSameAs(input);
    ArgumentCaptor<Object> update = ArgumentCaptor.forClass(Object.class);
    verify(socket).convertAndSend(eq("/topic/user/7"), update.capture());
    assertThat((ChatRoomUpdateDto) update.getValue())
        .satisfies(
            dto -> {
              assertThat(dto.unreadCount()).isEqualTo(2);
              assertThat(dto.lastMessage()).isEqualTo("content");
              assertThat(dto.senderId()).isEqualTo(7L);
            });
    verify(socket).convertAndSend(eq("/topic/user/8"), update.capture());
    assertThat(((ChatRoomUpdateDto) update.getValue()).unreadCount()).isEqualTo(2);
    verify(socket).convertAndSend(eq("/topic/user/9"), update.capture());
    assertThat(((ChatRoomUpdateDto) update.getValue()).unreadCount()).isZero();
    verifyNoInteractions(push);
  }

  @Test
  void soleParticipantDoesNotPushOrQueryMessageHistoryWithoutReadTime() {
    ChatRoom room = ChatRoom.builder().participants(Set.of(7L)).build();
    ChatMessage message = message("m1", 7L, TIME);
    when(messages.processWebSocketMessage(message, 7L, "room")).thenReturn(message);
    when(rooms.updateRoomActivity("room")).thenReturn(room);
    when(reads.getLastReadTimestampsBatchForUsers(List.of(7L), "room")).thenReturn(Map.of());
    service.processWebSocketMessage(message, 7L, "room");
    verify(messages, never()).loadMessagesAfterJoinTime(any(), any());
    verifyNoInteractions(push, members, sessions);
  }

  @Test
  void groupRoomListIncludesGroupTypeAndMissingLastMessageWithoutUnreadLookup() {
    ChatRoom room = ChatRoom.createWithParticipantsAndTitle(Set.of(8L), 7L, "group");
    when(reads.getLastReadTimestampsBatch(7L, List.of(room.getRoomId()))).thenReturn(Map.of());
    when(messages.getLastMessageTextsBatch(List.of(room.getRoomId()))).thenReturn(Map.of());
    assertThat(service.buildRoomListResponses(List.of(room), 7L))
        .singleElement()
        .satisfies(
            row -> {
              assertThat(row.getRoomType()).isEqualTo("group");
              assertThat(row.getParticipantCount()).isEqualTo(2);
              assertThat(row.getUnreadCount()).isZero();
              assertThat(row.getLastMessage()).isNull();
            });
  }

  private ChatMessage message(String id, Long sender, LocalDateTime time) {
    return ChatMessage.builder()
        .messageId(id)
        .roomId("room")
        .senderId(sender)
        .timestamp(time)
        .content("content")
        .build();
  }
}
