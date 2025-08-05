package com.dongsoop.dongsoop.chat.dto;

import com.dongsoop.dongsoop.chat.entity.ChatRoom;
import com.dongsoop.dongsoop.memberblock.constant.BlockType;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ChatRoomOverview {

    private Set<Long> participants;
    private String roomId;
    private String title;
    private Long managerId;
    private boolean isGroupChat;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivityAt;
    private BlockType blockType;

    public ChatRoomOverview(ChatRoom chatRoom, BlockType blockType) {
        this.roomId = chatRoom.getRoomId();
        this.title = chatRoom.getTitle();
        this.managerId = chatRoom.getManagerId();
        this.isGroupChat = chatRoom.isGroupChat();
        this.createdAt = chatRoom.getCreatedAt();
        this.lastActivityAt = chatRoom.getLastActivityAt();
        this.participants.addAll(chatRoom.getParticipants());
        this.blockType = blockType;
    }
}
