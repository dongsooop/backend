package com.dongsoop.dongsoop.chat;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.chat.entity.ChatRoomEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRoomTest {

    @Test
    @DisplayName("create()는 roomId를 non-null로 설정한다")
    void create_setsRoomIdNonNull() {
        ChatRoom room = ChatRoom.create(1L, 2L, "테스트 채팅방");

        assertThat(room.getRoomId()).isNotNull();
    }

    @Test
    @DisplayName("create()는 두 사용자를 모두 participants에 포함시킨다")
    void create_setsBothUsersAsParticipants() {
        ChatRoom room = ChatRoom.create(1L, 2L, "테스트 채팅방");

        assertThat(room.getParticipants()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("create()는 createdAt을 non-null로 설정한다")
    void create_setsCreatedAtNonNull() {
        ChatRoom room = ChatRoom.create(1L, 2L, "테스트 채팅방");

        assertThat(room.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("createWithParticipantsAndTitle()는 생성자를 participants에 포함시킨다")
    void createWithParticipantsAndTitle_includesCreator() {
        Set<Long> participants = Set.of(2L, 3L);
        Long creatorId = 1L;

        ChatRoom room = ChatRoom.createWithParticipantsAndTitle(participants, creatorId, "그룹 채팅");

        assertThat(room.getParticipants()).contains(creatorId);
        assertThat(room.getParticipants()).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("createWithParticipantsAndTitle()는 managerId를 생성자로 설정한다")
    void createWithParticipantsAndTitle_setsManagerIdToCreator() {
        Set<Long> participants = Set.of(2L, 3L);
        Long creatorId = 1L;

        ChatRoom room = ChatRoom.createWithParticipantsAndTitle(participants, creatorId, "그룹 채팅");

        assertThat(room.getManagerId()).isEqualTo(creatorId);
    }

    @Test
    @DisplayName("createWithParticipantsAndTitle()는 isGroupChat을 true로 설정한다")
    void createWithParticipantsAndTitle_setsIsGroupChatTrue() {
        Set<Long> participants = Set.of(2L, 3L);
        Long creatorId = 1L;

        ChatRoom room = ChatRoom.createWithParticipantsAndTitle(participants, creatorId, "그룹 채팅");

        assertThat(room.isGroupChat()).isTrue();
    }

    @Test
    @DisplayName("addNewParticipant()는 새로운 사용자를 participants에 추가한다")
    void addNewParticipant_addsUser() {
        ChatRoom room = ChatRoom.create(1L, 2L, "테스트 채팅방");

        room.addNewParticipant(3L);

        assertThat(room.getParticipants()).contains(3L);
        assertThat(room.getParticipantJoinTimes()).containsKey(3L);
    }

    @Test
    @DisplayName("kickUser()는 participants에서 제거하고 kickedUsers에 추가한다")
    void kickUser_removesFromParticipantsAndAddsToKickedUsers() {
        Set<Long> participants = Set.of(2L, 3L);
        Long creatorId = 1L;
        ChatRoom room = ChatRoom.createWithParticipantsAndTitle(participants, creatorId, "그룹 채팅");

        room.kickUser(3L);

        assertThat(room.getParticipants()).doesNotContain(3L);
        assertThat(room.getKickedUsers()).contains(3L);
    }

    @Test
    @DisplayName("isKicked()는 추방된 사용자에 대해 true를 반환한다")
    void isKicked_returnsTrueForKickedUser() {
        Set<Long> participants = Set.of(2L, 3L);
        Long creatorId = 1L;
        ChatRoom room = ChatRoom.createWithParticipantsAndTitle(participants, creatorId, "그룹 채팅");

        room.kickUser(3L);

        assertThat(room.isKicked(3L)).isTrue();
        assertThat(room.isKicked(1L)).isFalse();
    }

    @Test
    @DisplayName("toChatRoomEntity()는 roomId와 필드 값을 보존한다")
    void toChatRoomEntity_preservesRoomIdAndFields() {
        Set<Long> participants = Set.of(2L, 3L);
        Long creatorId = 1L;
        String title = "그룹 채팅";
        ChatRoom room = ChatRoom.createWithParticipantsAndTitle(participants, creatorId, title);

        ChatRoomEntity entity = room.toChatRoomEntity();

        assertThat(entity.getRoomId()).isEqualTo(room.getRoomId());
        assertThat(entity.getTitle()).isEqualTo(title);
        assertThat(entity.isGroupChat()).isTrue();
        assertThat(entity.getManagerId()).isEqualTo(creatorId);
        assertThat(entity.getParticipants()).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(entity.getCreatedAt()).isEqualTo(room.getCreatedAt());
    }
}
