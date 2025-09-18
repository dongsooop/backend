package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.util.List;

public interface FCMService {

    void subscribeTopic(List<String> token, String topic);

    void unsubscribeTopic(List<String> token, String topic);

    void sendNotification(List<String> fcmTokenList, NotificationSend notificationSend, Integer badge);

    void sendMessages(MulticastMessage message, List<String> tokens);

    void sendMessage(Message message);

    Notification getNotification(String title, String body);

    AndroidConfig getAndroidConfig(NotificationSend notificationSend, Integer badge);

    ApnsConfig getApnsConfig(NotificationSend notificationSend, Integer badge);

    Aps getAps(String title, String body, Integer badge);

    void updateNotificationBadge(List<String> deviceTokens, int badge);
}
