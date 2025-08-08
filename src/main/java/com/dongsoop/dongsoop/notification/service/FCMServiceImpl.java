package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.exception.NotificationSendException;
import com.google.api.core.ApiFuture;
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
public class FCMServiceImpl implements FCMService {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public void sendNotification(List<String> fcmTokenList, String title, String body) {
        // iOS용 APNs 설정
        ApnsConfig apnsConfig = getApnsConfig(title, body);

        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(fcmTokenList)
                .setApnsConfig(apnsConfig)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();

        sendMessage(message);
    }

    public ApnsConfig getApnsConfig(String title, String body) {
        return ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setAlert(ApsAlert.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .setSound("default")
                        .build())
                .build();
    }

    private void sendMessage(MulticastMessage message) {
        try {
            BatchResponse batchResponse = firebaseMessaging.sendEachForMulticast(message);
            log.info("Successfully sent message: {}", batchResponse);
        } catch (FirebaseMessagingException exception) {
            log.error("Failed to send message: {}", exception.getMessage());
            throw new NotificationSendException(exception);
        }
    }

    private void sendMessage(Message message) {
        try {
            String response = firebaseMessaging.send(message);
            log.info("Successfully sent message: {}", response);
        } catch (FirebaseMessagingException exception) {
            log.error("Failed to send message: {}", exception.getMessage());
            throw new NotificationSendException(exception);
        }
    }

    public void sendMessages(List<Message> messageList) {
        ApiFuture<BatchResponse> batchResponseApiFuture = firebaseMessaging.sendEachAsync(messageList);
        if (batchResponseApiFuture.isCancelled()) {
            log.error("Failed to send messages: {}", batchResponseApiFuture);
            throw new NotificationSendException();
        }

        if (batchResponseApiFuture.isDone()) {
            log.info("Successfully sent messages: {}", batchResponseApiFuture);
        }
    }
}
