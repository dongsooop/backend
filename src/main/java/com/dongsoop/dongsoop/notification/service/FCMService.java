package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.MulticastMessage;
import java.util.List;

public interface FCMService {

    void sendNotification(List<String> fcmTokenList, NotificationSend notificationSend);

    void sendMessages(MulticastMessage message, List<String> tokens);

    ApnsConfig getApnsConfig(NotificationSend notificationSend);

    Aps getAps(String title, String body, int badge);

    void updateNotificationBadge(List<String> deviceTokens, int badge);
}
