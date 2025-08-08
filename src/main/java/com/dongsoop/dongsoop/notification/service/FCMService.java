package com.dongsoop.dongsoop.notification.service;

import java.util.List;

public interface FCMService {

    void sendNotification(String fcmToken, String title, String body);

    void sendNotification(List<String> fcmTokenList, String title, String body);
}
