package com.dongsoop.dongsoop.chat;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.exception.BoardNotFoundException;
import com.dongsoop.dongsoop.chat.exception.SelfChatException;
import com.dongsoop.dongsoop.chat.repository.RedisChatRepository;
import com.dongsoop.dongsoop.chat.service.*;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.search.entity.BoardType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatRoomLifecycleContractTest {
    @Mock RedisChatRepository redis;
    @Mock ChatSyncService sync;
    @Mock ProjectBoardRepository projects;
    @Mock StudyBoardRepository studies;
    @Mock TutoringBoardRepository tutorings;
    @Mock MarketplaceBoardRepository market;
    @Mock ContactRoomMappingService mappings;
    ChatRoomService service;

    @BeforeEach
    void setup() {
        service = new ChatRoomService(redis, new ChatValidator(sync, null), sync, projects, studies, tutorings, market, mappings);
    }

    @Test
    void directRoomCreatesParticipantsAndRejectsSelfOrNonPositiveIds() {
        when(redis.saveRoom(any())).thenAnswer(call -> call.getArgument(0));
        ChatRoom room = service.createOneToOneChatRoom(7L, 8L, "title");
        assertThat(room.getParticipants()).containsExactlyInAnyOrder(7L, 8L);
        assertThat(room.getTitle()).isEqualTo("title");
        assertThat(room.isGroupChat()).isFalse();
        assertThat(room.getParticipantJoinTimes()).containsOnlyKeys(7L, 8L);
        assertThatThrownBy(() -> service.createOneToOneChatRoom(7L, 7L, "title")).isInstanceOf(SelfChatException.class);
        assertThatThrownBy(() -> service.createOneToOneChatRoom(0L, 8L, "title"))
                .isInstanceOf(com.dongsoop.dongsoop.chat.exception.UnauthorizedChatAccessException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "existing"})
    void reusingDirectRoomOnlyFillsMissingTitle(String title) {
        ChatRoom existing = ChatRoom.create(7L, 8L, title);
        when(redis.findRoomByParticipants(7L, 8L)).thenReturn(Optional.of(existing));
        when(redis.saveRoom(existing)).thenReturn(existing);
        assertThat(service.createOneToOneChatRoom(7L, 8L, "requested")).isSameAs(existing);
        String expected = "requested";
        if ("existing".equals(title)) { expected = "existing"; }
        assertThat(existing.getTitle()).isEqualTo(expected);
    }

    @Test
    void groupRoomAddsCreatorAndRejectsEmptyParticipants() {
        when(redis.saveRoom(any())).thenAnswer(call -> call.getArgument(0));
        ChatRoom group = service.createGroupChatRoom(7L, Set.of(8L, 9L), "group");
        assertThat(group.isGroupChat()).isTrue();
        assertThat(group.getManagerId()).isEqualTo(7L);
        assertThat(group.getParticipants()).containsExactlyInAnyOrder(7L, 8L, 9L);
        assertThatIllegalArgumentException().isThrownBy(() -> service.createGroupChatRoom(7L, Set.of(), "group"));
    }

    @ParameterizedTest
    @EnumSource(value = BoardType.class, names = {"PROJECT", "STUDY", "TUTORING", "MARKETPLACE", "BLINDDATE"})
    void contactRoomUsesBoardSpecificTitleAndStoresMapping(BoardType type) {
        existingBoard(type);
        when(redis.saveRoom(any())).thenAnswer(call -> call.getArgument(0));
        ChatRoom room = service.createContactChatRoom(7L, 8L, type, 9L, "board");
        String prefix = ChatRoom.CONTACT_ROOM_TITLE_PREFIX;
        if (type == BoardType.MARKETPLACE) { prefix = ChatRoom.TRADE_ROOM_TITLE_PREFIX; }
        if (type == BoardType.BLINDDATE) { prefix = ChatRoom.BLINDDATE_ROOM_TITLE_PREFIX; }
        assertThat(room.getTitle()).isEqualTo(prefix + " board");
        assertThat(room.getParticipants()).containsExactlyInAnyOrder(7L, 8L);
        verify(mappings).saveContactRoomMapping(7L, 8L, type, 9L, room.getRoomId());
    }

    @ParameterizedTest
    @EnumSource(value = BoardType.class, names = {"PROJECT", "STUDY", "TUTORING", "NOTICE"})
    void absentOrUnsupportedBoardCannotCreateContactRoom(BoardType type) {
        assertThatThrownBy(() -> service.createContactChatRoom(7L, 8L, type, 9L, "board"))
                .isInstanceOf(BoardNotFoundException.class);
        verifyNoInteractions(redis, mappings);
    }

    @Test
    void existingContactMappingRestoresRoomWithoutNewStorage() {
        when(mappings.findExistingContactRoomId(7L, 8L, BoardType.MARKETPLACE, 9L)).thenReturn("existing");
        ChatRoom existing = ChatRoom.create(7L, 8L, "existing");
        when(sync.findRoomOrRestore("existing")).thenReturn(existing);
        assertThat(service.createContactChatRoom(7L, 8L, BoardType.MARKETPLACE, 9L, "board")).isSameAs(existing);
        verifyNoInteractions(redis);
    }

    @Test
    void mappingFailureDeletesJustCreatedRoomAndPropagatesError() {
        when(redis.saveRoom(any())).thenAnswer(call -> call.getArgument(0));
        doThrow(new IllegalStateException("mapping failed")).when(mappings)
                .saveContactRoomMapping(eq(7L), eq(8L), eq(BoardType.MARKETPLACE), eq(9L), any());
        assertThatThrownBy(() -> service.createContactChatRoom(7L, 8L, BoardType.MARKETPLACE, 9L, "board"))
                .isInstanceOf(IllegalStateException.class);
        ArgumentCaptor<ChatRoom> saved = ArgumentCaptor.forClass(ChatRoom.class);
        verify(redis).saveRoom(saved.capture());
        verify(redis).deleteRoom(saved.getValue().getRoomId());
    }

    @Test
    void roomListExcludesKickedAndNonParticipants() {
        ChatRoom accessible = ChatRoom.create(7L, 8L, "accessible");
        ChatRoom kicked = ChatRoom.create(7L, 8L, "kicked");
        kicked.getKickedUsers().add(7L);
        ChatRoom unrelated = ChatRoom.create(8L, 9L, "unrelated");
        when(redis.findRoomsByUserId(7L)).thenReturn(List.of(accessible, kicked, unrelated));
        assertThat(service.getRoomsForUserId(7L)).containsExactly(accessible);
    }

    @Test
    void voluntaryContactLeaveRemovesMembershipAndMappingWithoutKick() {
        ChatRoom room = ChatRoom.create(7L, 8L, "contact");
        room.setLastActivityAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        when(sync.findRoomOrRestore("contact")).thenReturn(room);
        service.handleContactRoomLeave("contact", 7L);
        assertThat(room.getParticipants()).containsExactly(8L);
        assertThat(room.getParticipantJoinTimes()).doesNotContainKey(7L);
        assertThat(room.isKicked(7L)).isFalse();
        assertThat(room.getLastActivityAt()).isAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
        verify(mappings).deleteContactRoomMapping("contact");
        verify(redis).saveRoom(room);
    }

    @Test
    void roomActivityUpdatePersistsRestoredRoom() {
        ChatRoom room = ChatRoom.create(7L, 8L, "room");
        room.setLastActivityAt(LocalDateTime.of(2020, 1, 1, 0, 0));
        when(sync.findRoomOrRestore("room")).thenReturn(room);
        when(redis.saveRoom(room)).thenReturn(room);
        assertThat(service.updateRoomActivity("room")).isSameAs(room);
        assertThat(room.getLastActivityAt()).isAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
    }

    private void existingBoard(BoardType type) {
        if (type == BoardType.PROJECT) { when(projects.existsById(9L)).thenReturn(true); }
        if (type == BoardType.STUDY) { when(studies.existsById(9L)).thenReturn(true); }
        if (type == BoardType.TUTORING) { when(tutorings.existsById(9L)).thenReturn(true); }
    }
}
