package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.entity.ChatMessage;
import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.exception.*;
import com.dongsoop.dongsoop.chat.repository.ChatRepository;
import com.dongsoop.dongsoop.chat.service.ChatSyncService;
import com.dongsoop.dongsoop.chat.util.ChatCommonUtils;
import com.dongsoop.dongsoop.chat.validator.ChatValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatValidatorTest {

    @Mock
    private ChatRepository chatRepository;
    @Mock
    private ChatSyncService chatSyncService;
    @Mock
    private ChatCommonUtils chatCommonUtils;

    @InjectMocks
    private ChatValidator chatValidator;

    @Test
    @DisplayName("validateSelfChat - 같은 사용자이면 SelfChatException")
    void validateSelfChat_throwsOnSameUser() {
        assertThatThrownBy(() -> chatValidator.validateSelfChat(1L, 1L))
                .isInstanceOf(SelfChatException.class);
    }

    @Test
    @DisplayName("validateSelfChat - 다른 사용자이면 성공")
    void validateSelfChat_succeedsForDifferentUsers() {
        chatValidator.validateSelfChat(1L, 2L);
    }

    @Test
    @DisplayName("validateAndEnrichMessage - roomId 없으면 InvalidChatRequestException")
    void validateAndEnrichMessage_throwsOnMissingRoomId() {
        ChatMessage message = ChatMessage.builder().senderId(1L).build();

        assertThatThrownBy(() -> chatValidator.validateAndEnrichMessage(message))
                .isInstanceOf(InvalidChatRequestException.class);
    }

    @Test
    @DisplayName("validateAndEnrichMessage - senderId 없으면 InvalidChatRequestException")
    void validateAndEnrichMessage_throwsOnMissingSenderId() {
        ChatMessage message = ChatMessage.builder().roomId("room1").build();

        assertThatThrownBy(() -> chatValidator.validateAndEnrichMessage(message))
                .isInstanceOf(InvalidChatRequestException.class);
    }

    @Test
    @DisplayName("validateAndEnrichMessage - 유효한 메시지 enrichment")
    void validateAndEnrichMessage_enrichesValidMessage() {
        ChatMessage message = ChatMessage.builder()
                .roomId("room1")
                .senderId(1L)
                .content("hello")
                .build();

        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participants(new HashSet<>(Set.of(1L)))
                .kickedUsers(new HashSet<>())
                .build();
        when(chatSyncService.findRoomOrRestore("room1")).thenReturn(room);

        ChatMessage result = chatValidator.validateAndEnrichMessage(message);

        assertThat(result.getMessageId()).isNotNull();
        assertThat(result.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("validateManagerPermission - 그룹이 아닌 방에서 GroupChatOnlyException")
    void validateManagerPermission_throwsForNonGroup() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .isGroupChat(false)
                .build();

        assertThatThrownBy(() -> chatValidator.validateManagerPermission(room, 1L))
                .isInstanceOf(GroupChatOnlyException.class);
    }

    @Test
    @DisplayName("validateManagerPermission - 매니저가 아니면 UnauthorizedManagerActionException")
    void validateManagerPermission_throwsForNonManager() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .isGroupChat(true)
                .managerId(1L)
                .build();

        assertThatThrownBy(() -> chatValidator.validateManagerPermission(room, 2L))
                .isInstanceOf(UnauthorizedManagerActionException.class);
    }

    @Test
    @DisplayName("validateKickableUser - 방에 없는 사용자 UserNotInRoomException")
    void validateKickableUser_throwsForAbsentUser() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participants(new HashSet<>(Set.of(1L)))
                .managerId(1L)
                .build();

        assertThatThrownBy(() -> chatValidator.validateKickableUser(room, 2L))
                .isInstanceOf(UserNotInRoomException.class);
    }

    @Test
    @DisplayName("validateKickableUser - 매니저 강퇴 시도 시 ManagerKickAttemptException")
    void validateKickableUser_throwsForKickingManager() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participants(new HashSet<>(Set.of(1L, 2L)))
                .managerId(1L)
                .build();

        assertThatThrownBy(() -> chatValidator.validateKickableUser(room, 1L))
                .isInstanceOf(ManagerKickAttemptException.class);
    }

    @Test
    @DisplayName("validateUserForRoom - 강퇴된 사용자 UserKickedException")
    void validateUserForRoom_throwsForKickedUser() {
        ChatRoom room = ChatRoom.builder()
                .roomId("room1")
                .participants(new HashSet<>(Set.of(1L)))
                .kickedUsers(new HashSet<>(Set.of(2L)))
                .build();
        when(chatSyncService.findRoomOrRestore("room1")).thenReturn(room);

        assertThatThrownBy(() -> chatValidator.validateUserForRoom("room1", 2L))
                .isInstanceOf(UserKickedException.class);
    }
}
