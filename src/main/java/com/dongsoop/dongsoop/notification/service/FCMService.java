package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.util.List;

public interface FCMService {

    void sendNotification(List<String> fcmTokenList, NotificationSend notificationSend, Integer badge);

    void sendMessages(MulticastMessage message, List<String> tokens);

    Notification getNotification(String title, String body);

    ApnsConfig getApnsConfig(NotificationSend notificationSend, Integer badge);

    Aps getAps(String title, String body, Integer badge);

    void updateNotificationBadge(List<String> deviceTokens, int badge);
}
