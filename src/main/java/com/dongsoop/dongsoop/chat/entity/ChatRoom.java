package com.dongsoop.dongsoop.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {
    private static final int BACKUP_DAYS_THRESHOLD = 25;

    private String roomId;
    private Set<String> participants;
    private String managerId;
    private boolean isGroupChat;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private Set<String> kickedUsers = new HashSet<>();

    public static ChatRoom create(String user1, String user2) {
        String roomId = UUID.randomUUID().toString();
        Set<String> participants = new HashSet<>();
        participants.add(user1);
        participants.add(user2);
        LocalDateTime now = LocalDateTime.now();

        return ChatRoom.builder()
                .roomId(roomId)
                .participants(participants)
                .createdAt(now)
                .lastActivityAt(now)
                .build();
    }

    public static ChatRoom createWithParticipants(Set<String> participants, String creatorId) {
        String roomId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        return ChatRoom.builder()
                .roomId(roomId)
                .participants(new HashSet<>(participants))
                .managerId(creatorId)
                .isGroupChat(true)
                .createdAt(now)
                .lastActivityAt(now)
                .build();
    }

    public ChatRoomEntity toChatRoomEntity() {
        LocalDateTime effectiveCreatedAt = Optional.ofNullable(this.createdAt)
                .orElseGet(() -> LocalDateTime.now().minusDays(BACKUP_DAYS_THRESHOLD));

        LocalDateTime effectiveLastActivityAt = Optional.ofNullable(this.lastActivityAt)
                .orElseGet(LocalDateTime::now);

        return ChatRoomEntity.builder()
                .roomId(this.roomId)
                .isGroupChat(this.isGroupChat)
                .managerId(this.managerId)
                .participants(new HashSet<>(this.participants))
                .createdAt(effectiveCreatedAt)
                .lastActivityAt(effectiveLastActivityAt)
                .build();
    }

    public void updateActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public void kickUser(String userId) {
        participants.remove(userId);
        if (kickedUsers == null) {
            kickedUsers = new HashSet<>();
        }
        kickedUsers.add(userId);
        updateActivity();
    }

    public boolean isKicked(String userId) {
        if (kickedUsers == null) {
            kickedUsers = new HashSet<>();
        }
        return kickedUsers.contains(userId);
    }
}