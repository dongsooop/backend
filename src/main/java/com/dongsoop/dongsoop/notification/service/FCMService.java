package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import java.util.List;

public interface FCMService {

    void sendToTopic(String topic, NotificationSend notificationSend);

    void subscribeTopic(List<String> token, String topic);

    void unsubscribeTopic(List<String> token, String topic);

    void sendNotification(List<String> fcmTokenList, NotificationSend notificationSend, Integer badge);

    void sendMessages(MulticastMessage message, List<String> tokens);

    void sendMessage(Message message);

    void updateNotificationBadge(List<String> deviceTokens, int badge);
}
