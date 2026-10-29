package com.dongsoop.dongsoop.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatMessageEntity;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomEntity;
import com.dongsoop.dongsoop.chat.entity.MessageType;
import com.dongsoop.dongsoop.chat.exception.ChatRoomNotFoundException;
import com.dongsoop.dongsoop.chat.repository.ChatMessageJpaRepository;
import com.dongsoop.dongsoop.chat.repository.ChatRoomJpaRepository;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatSyncService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatSyncRecoveryTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 12, 0);
  @Mock RedisChatRepository redis;
  @Mock ChatRoomJpaRepository rooms;
  @Mock ChatMessageJpaRepository messages;
  @InjectMocks ChatSyncService service;

  @Test
  void cachedRoomDoesNotQueryDatabase() {
    ChatRoom cached = ChatRoom.builder().roomId("room-7").build();
    when(redis.findRoomById("room-7")).thenReturn(Optional.of(cached));
    assertThat(service.findRoomOrRestore("room-7")).isSameAs(cached);
    verifyNoInteractions(rooms, messages);
  }

  @Test
  void missingRoomIsNullForGroupRestoreAndErrorForRequiredLookup() {
    assertThat(service.restoreGroupChatRoom("missing")).isNull();
    assertThatThrownBy(() -> service.findRoomOrRestore("missing"))
        .isInstanceOf(ChatRoomNotFoundException.class);
    verifyNoInteractions(messages);
  }

  @Test
  void databaseRoomAndMessagesRestoreAllStoredValues() {
    ChatRoomEntity stored =
        ChatRoomEntity.builder()
            .roomId("room-7")
            .title("모집 채팅")
            .isGroupChat(true)
            .managerId(7L)
            .participants(Set.of(7L, 8L))
            .createdAt(NOW.minusDays(1))
            .lastActivityAt(NOW)
            .build();
    when(rooms.findById("room-7")).thenReturn(Optional.of(stored));
    when(messages.findByRoomIdOrderByTimestampAsc("room-7"))
        .thenReturn(List.of(message("m1", 7L, NOW.minusMinutes(1)), message("m2", 8L, NOW)));
    ChatRoom restored = service.findRoomOrRestore("room-7");
    assertThat(restored.getRoomId()).isEqualTo("room-7");
    assertThat(restored.getTitle()).isEqualTo("모집 채팅");
    assertThat(restored.isGroupChat()).isTrue();
    assertThat(restored.getManagerId()).isEqualTo(7L);
    assertThat(restored.getParticipants()).containsExactlyInAnyOrder(7L, 8L);
    assertThat(restored.getCreatedAt()).isEqualTo(NOW.minusDays(1));
    assertThat(restored.getLastActivityAt()).isEqualTo(NOW);
    verify(redis).saveRoom(restored);
    ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
    verify(redis, times(2)).saveMessage(saved.capture());
    assertThat(saved.getAllValues())
        .extracting(ChatMessage::getMessageId)
        .containsExactly("m1", "m2");
    assertThat(saved.getAllValues()).extracting(ChatMessage::getSenderId).containsExactly(7L, 8L);
    assertThat(saved.getAllValues())
        .extracting(ChatMessage::getTimestamp)
        .containsExactly(NOW.minusMinutes(1), NOW);
    assertThat(saved.getAllValues())
        .allSatisfy(
            value -> {
              assertThat(value.getRoomId()).isEqualTo("room-7");
              assertThat(value.getContent()).isEqualTo("내용");
              assertThat(value.getType()).isEqualTo(MessageType.CHAT);
            });
  }

  @Test
  void emptyMessageHistoryStillRestoresRoom() {
    when(rooms.findById("room-7"))
        .thenReturn(Optional.of(ChatRoomEntity.builder().roomId("room-7").build()));
    when(messages.findByRoomIdOrderByTimestampAsc("room-7")).thenReturn(List.of());
    assertThat(service.restoreGroupChatRoom("room-7").getRoomId()).isEqualTo("room-7");
    verify(redis).saveRoom(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void redisRoomWriteFailureStopsMessageRestore() {
    when(rooms.findById("room-7"))
        .thenReturn(Optional.of(ChatRoomEntity.builder().roomId("room-7").build()));
    doThrow(new IllegalStateException("redis unavailable"))
        .when(redis)
        .saveRoom(org.mockito.ArgumentMatchers.any());
    assertThatThrownBy(() -> service.restoreGroupChatRoom("room-7"))
        .isInstanceOf(IllegalStateException.class);
    verifyNoInteractions(messages);
  }

  private ChatMessageEntity message(String id, Long sender, LocalDateTime time) {
    return ChatMessageEntity.builder()
        .messageId(id)
        .roomId("room-7")
        .senderId(sender)
        .content("내용")
        .timestamp(time)
        .type(MessageType.CHAT)
        .build();
  }
}
