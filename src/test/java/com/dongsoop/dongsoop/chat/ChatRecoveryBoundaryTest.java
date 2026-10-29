package com.dongsoop.dongsoop.chat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.chat.entity.*;
import com.dongsoop.dongsoop.chat.exception.*;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.service.*;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.member.service.MemberService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatRecoveryBoundaryTest {
  private static final LocalDateTime TIME = LocalDateTime.of(2026, 10, 15, 12, 0);
  @Mock RedisChatRepository redis;
  @Mock ChatValidator validator;
  @Mock MemberService members;
  @Mock ChatRoomService rooms;
  @Mock ReadStatusService reads;
  @Mock ChatMessageService system;
  ChatMessageService messages;
  ChatParticipantService participants;

  @BeforeEach
  void setup() {
    messages = new ChatMessageService(redis, validator, members);
    participants = new ChatParticipantService(validator, reads, rooms, system);
  }

  @Test
  void offlineSyncIgnoresNullOverridesUntrustedIdentityAndPreservesExistingIdAndTime() {
    ChatMessage existing =
        ChatMessage.builder()
            .messageId("existing")
            .timestamp(TIME)
            .senderId(99L)
            .roomId("forged")
            .content("old")
            .type(MessageType.CHAT)
            .build();
    ChatMessage fresh = ChatMessage.builder().content("new").build();
    var result = messages.processOfflineMessages("room", 7L, Arrays.asList(null, existing, fresh));
    assertThat(result)
        .containsExactly(existing, fresh)
        .allSatisfy(
            value -> {
              assertThat(value.getRoomId()).isEqualTo("room");
              assertThat(value.getSenderId()).isEqualTo(7L);
              assertThat(value.getMessageId()).isNotBlank();
              assertThat(value.getTimestamp()).isNotNull();
              assertThat(value.getType()).isEqualTo(MessageType.CHAT);
            });
    assertThat(existing.getMessageId()).isEqualTo("existing");
    assertThat(existing.getTimestamp()).isEqualTo(TIME);
    ArgumentCaptor<ChatMessage> saved = ArgumentCaptor.forClass(ChatMessage.class);
    verify(redis, times(2)).saveMessage(saved.capture());
    assertThat(saved.getAllValues())
        .extracting(ChatMessage::getContent)
        .containsExactly("old", "new");
  }

  @Test
  void socketMessageUsesAuthenticatedSenderAndRoomBeforeSave() {
    ChatMessage input =
        ChatMessage.builder().senderId(99L).roomId("forged").content("body").build();
    when(validator.validateAndEnrichMessage(input)).thenAnswer(call -> call.getArgument(0));
    assertThat(messages.processWebSocketMessage(input, 7L, "room")).isSameAs(input);
    assertThat(input.getSenderId()).isEqualTo(7L);
    assertThat(input.getRoomId()).isEqualTo("room");
    verify(redis).saveMessage(input);
  }

  @Test
  void cursorChoosesFullHistoryOrOnlyNewMessages() {
    ChatMessage old = ChatMessage.builder().messageId("old").build();
    ChatMessage newer = ChatMessage.builder().messageId("new").build();
    when(redis.findMessagesByRoomId("room")).thenReturn(List.of(old, newer));
    when(redis.findMessagesByRoomIdAfterId("room", "old")).thenReturn(List.of(newer));
    assertThat(messages.loadNewMessages("room", null)).containsExactly(old, newer);
    assertThat(messages.loadNewMessages("room", "old")).containsExactly(newer);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void invitedMemberEntryIsOnlyCreatedUntilOwnEnterMessageExists(boolean entered) {
    ChatRoom room = ChatRoom.createWithParticipantsAndTitle(Set.of(8L), 7L, "group");
    room.setCreatedAt(TIME.minusHours(1));
    room.getParticipantJoinTimes().put(8L, TIME);
    when(rooms.getChatRoomById("room")).thenReturn(room);
    ChatMessage otherEntry = ChatMessage.builder().senderId(7L).type(MessageType.ENTER).build();
    ChatMessage own = ChatMessage.builder().senderId(8L).type(MessageType.CHAT).build();
    if (entered) {
      own.setType(MessageType.ENTER);
    }
    when(system.loadMessagesAfterJoinTime("room", TIME)).thenReturn(List.of(otherEntry, own));
    ChatMessage expected = null;
    if (!entered) {
      expected = ChatMessage.builder().type(MessageType.ENTER).build();
      when(system.createAndSaveSystemMessage("room", 8L, MessageType.ENTER)).thenReturn(expected);
    }
    assertThat(participants.checkFirstTimeEntryAndCreateEnterMessage("room", 8L))
        .isSameAs(expected);
    if (entered) {
      verify(system, never()).createAndSaveSystemMessage(any(), any(), any());
    }
  }

  @Test
  void missingJoinMetadataCreatesEntryButOriginalParticipantDoesNot() {
    ChatRoom room = ChatRoom.create(7L, 8L, "room");
    room.setCreatedAt(TIME);
    room.getParticipantJoinTimes().put(8L, TIME);
    when(rooms.getChatRoomById("room")).thenReturn(room);
    assertThat(participants.checkFirstTimeEntryAndCreateEnterMessage("room", 8L)).isNull();
    room.getParticipantJoinTimes().remove(8L);
    ChatMessage entry = ChatMessage.builder().type(MessageType.ENTER).build();
    when(system.createAndSaveSystemMessage("room", 8L, MessageType.ENTER)).thenReturn(entry);
    assertThat(participants.checkFirstTimeEntryAndCreateEnterMessage("room", 8L)).isSameAs(entry);
  }

  @ParameterizedTest
  @ValueSource(longs = {8, 9, 0})
  void invalidInviteCannotChangeMembershipOrReadState(Long target) {
    ChatRoom room = ChatRoom.createWithParticipantsAndTitle(Set.of(8L), 7L, "group");
    room.getKickedUsers().add(9L);
    when(rooms.getChatRoomById("room")).thenReturn(room);
    Class<? extends RuntimeException> error = UnauthorizedChatAccessException.class;
    if (target == 8L) {
      error = UserAlreadyInRoomException.class;
    }
    if (target == 9L) {
      error = KickedUserInviteException.class;
    }
    ChatParticipantService checked =
        new ChatParticipantService(new ChatValidator(null, null), reads, rooms, system);
    assertThatThrownBy(() -> checked.inviteUserToGroupChat("room", 7L, target)).isInstanceOf(error);
    assertThat(room.getParticipants()).containsExactlyInAnyOrder(7L, 8L);
    verifyNoInteractions(reads, system);
    verify(rooms, never()).saveRoom(any());
  }

  @Test
  void nonManagerCannotInviteOrMutateParticipants() {
    ChatRoom room = ChatRoom.createWithParticipantsAndTitle(Set.of(8L), 7L, "group");
    when(rooms.getChatRoomById("room")).thenReturn(room);
    ChatParticipantService checked =
        new ChatParticipantService(new ChatValidator(null, null), reads, rooms, system);
    assertThatThrownBy(() -> checked.inviteUserToGroupChat("room", 8L, 9L))
        .isInstanceOf(UnauthorizedManagerActionException.class);
    assertThat(room.getParticipants()).containsExactlyInAnyOrder(7L, 8L);
    verifyNoInteractions(reads, system);
    verify(rooms, never()).saveRoom(any());
  }

  @Test
  void lastVoluntaryParticipantLeavesWithoutKickAndEmptyRoomIsDeleted() {
    ChatRoom room = ChatRoom.create(7L, 8L, "direct");
    room.leaveRoom(8L);
    room.setRoomId("room");
    when(rooms.getChatRoomById("room")).thenReturn(room);
    participants.leaveChatRoom("room", 7L);
    assertThat(room.getParticipants()).isEmpty();
    assertThat(room.isKicked(7L)).isFalse();
    verify(rooms).deleteRoom("room");
    verify(system).createAndSaveSystemMessage("room", 7L, MessageType.LEAVE);
  }
}
