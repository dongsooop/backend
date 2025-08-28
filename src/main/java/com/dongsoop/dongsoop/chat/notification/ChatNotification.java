package com.dongsoop.dongsoop.chat.notification;

import java.util.Set;

public interface ChatNotification {

    void send(Set<Long> chatroomMemberIdSet, String chatRoomId, String senderName,
              String message);
}
