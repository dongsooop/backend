package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Message;
import java.util.List;

public interface FCMService {

    void sendNotification(List<String> fcmTokenList, String title, String body, NotificationType type, String value);

    ApnsConfig getApnsConfig(String title, String body, NotificationType type, String value);

    void sendMessages(List<Message> messageList);
}
