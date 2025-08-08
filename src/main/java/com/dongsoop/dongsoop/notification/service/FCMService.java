package com.dongsoop.dongsoop.notification.service;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Message;
import java.util.List;

public interface FCMService {

    void sendNotification(List<String> fcmTokenList, String title, String body);

    ApnsConfig getApnsConfig(String title, String body);

    void sendMessages(List<Message> messageList);
}
