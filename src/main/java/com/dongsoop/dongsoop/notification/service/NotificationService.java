package com.dongsoop.dongsoop.notification.service;

import java.util.Set;

public interface NotificationService {

    void sendNotificationForChat(Set<Long> chatroomMemberIdSet, String roomId, String senderName, String message);
}
