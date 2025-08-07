package com.dongsoop.dongsoop.notification.service;

import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FCMService {

    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(String fcmToken, String title, String body)
            throws FirebaseMessagingException {

        // iOS용 APNs 설정
        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setAlert(ApsAlert.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setSound("default")
                        .setBadge(1)
                        .build())
                .build();

        Message message = Message.builder()
                .setToken(fcmToken)
                .setApnsConfig(apnsConfig)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        String response = firebaseMessaging.send(message);
        log.info("Successfully sent message: {}", response);
    }

    public void sendNotification(List<String> fcmTokenList, String title, String body)
            throws FirebaseMessagingException {

        // iOS용 APNs 설정
        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setAlert(ApsAlert.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setSound("default")
                        .setBadge(1)
                        .build())
                .build();

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(fcmTokenList)
                .setApnsConfig(apnsConfig)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        BatchResponse batchResponse = firebaseMessaging.sendEachForMulticast(message);
        log.info("Successfully sent message: {}", batchResponse);
    }
}
