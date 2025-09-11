package com.dongsoop.dongsoop.chat.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomListResponse {
    private String roomId;
    private String title;
    private int participantCount;
    private String lastMessage;
    private int unreadCount;
    private LocalDateTime lastActivityAt;
    private boolean isGroupChat;
    private String roomType;
}
