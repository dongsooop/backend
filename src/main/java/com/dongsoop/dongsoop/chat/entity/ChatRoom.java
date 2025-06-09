package com.dongsoop.dongsoop.chat.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    private static final int BACKUP_DAYS_THRESHOLD = 25;
    private static final String DEFAULT_GROUP_TITLE = "그룹 채팅";

    private String roomId;
    private String title;

    @Builder.Default
    private Set<Long> participants = new HashSet<>();

    private Long managerId;
    private boolean isGroupChat;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;

    @Builder.Default
    private Set<Long> kickedUsers = new HashSet<>();
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<Long, String> participantNicknames;

    public static ChatRoom create(Long user1, Long user2) {
        return ChatRoom.builder()
                .roomId(generateRandomRoomId())
                .participants(createParticipantSet(user1, user2))
                .createdAt(getCurrentTime())
                .lastActivityAt(getCurrentTime())
                .kickedUsers(new HashSet<>())
                .build();
    }

    public static ChatRoom createWithParticipantsAndTitle(Set<Long> participants, Long creatorId, String title) {
        return ChatRoom.builder()
                .roomId(generateRandomRoomId())
                .title(title)
                .participants(new HashSet<>(participants))
                .managerId(creatorId)
                .isGroupChat(true)
                .createdAt(getCurrentTime())
                .lastActivityAt(getCurrentTime())
                .kickedUsers(new HashSet<>())
                .build();
    }

    private static String generateRandomRoomId() {
        return UUID.randomUUID().toString();
    }

    private static Set<Long> createParticipantSet(Long user1, Long user2) {
        Set<Long> participants = new HashSet<>();
        participants.add(user1);
        participants.add(user2);
        return participants;
    }

    private static LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }

    public ChatRoomEntity toChatRoomEntity() {
        return ChatRoomEntity.builder()
                .roomId(this.roomId)
                .title(this.title)
                .isGroupChat(this.isGroupChat)
                .managerId(this.managerId)
                .participants(new HashSet<>(this.participants))
                .createdAt(getEffectiveCreatedAt())
                .lastActivityAt(getEffectiveLastActivityAt())
                .build();
    }

    public void updateActivity() {
        this.lastActivityAt = getCurrentTime();
    }

    public void kickUser(Long userId) {
        participants.remove(userId);
        ensureKickedUsersSet().add(userId);
        updateActivity();
    }

    public boolean isKicked(Long userId) {
        return ensureKickedUsersSet().contains(userId);
    }

    private LocalDateTime getEffectiveCreatedAt() {
        if (this.createdAt != null) {
            return this.createdAt;
        }
        return getCurrentTime().minusDays(BACKUP_DAYS_THRESHOLD);
    }

    private LocalDateTime getEffectiveLastActivityAt() {
        if (this.lastActivityAt != null) {
            return this.lastActivityAt;
        }
        return LocalDateTime.now();
    }

    private Set<Long> ensureKickedUsersSet() {
        if (kickedUsers == null) {
            kickedUsers = new HashSet<>();
        }
        return kickedUsers;
    }

    public void setParticipantNicknames(Map<Long, String> nicknames) {
        this.participantNicknames = nicknames;
    }

    public String getParticipantNickname(Long userId) {
        return Optional.ofNullable(participantNicknames)
                .map(nicknames -> nicknames.get(userId))
                .orElse(null);
    }

    public boolean isManager(Long userId) {
        return Optional.ofNullable(managerId)
                .map(id -> id.equals(userId))
                .orElse(false);
    }
}