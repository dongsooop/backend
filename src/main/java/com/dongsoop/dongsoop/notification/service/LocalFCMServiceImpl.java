package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.constant.FcmSilentType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Profile("local")
public class LocalFCMServiceImpl implements FCMService {
    
    @Override
    public void sendToTopic(String topic, NotificationSend notificationSend) {
        log.debug("Dev FCM Service - sendToTopic called. topic: {}, notificationSend: {}", topic, notificationSend);
    }

    @Override
    public void subscribeTopic(List<String> token, String topic) {
        log.debug("Dev FCM Service - subscribeTopic called. topic: {}, tokens: {}", topic, token);
    }

    @Override
    public void unsubscribeTopic(List<String> token, String topic) {
        log.debug("Dev FCM Service - unsubscribeTopic called. topic: {}, tokens: {}", topic, token);
    }

    @Override
    public void sendNotification(List<String> fcmTokenList, NotificationSend notificationSend, Integer badge) {
        log.debug("Dev FCM Service - sendNotification called. tokens: {}, notificationSend: {}, badge: {}",
                fcmTokenList, notificationSend, badge);
    }

    @Override
    public void sendMessages(MulticastMessage message, List<String> tokens) {
        log.debug("Dev FCM Service - sendMessages called. tokens: {}", tokens);
    }

    @Override
    public void sendMessage(Message message) {
        log.debug("Dev FCM Service - sendMessage called. message: {}", message);
    }

    @Override
    public void updateNotificationBadge(List<String> deviceTokens, int badge) {
        log.debug("Dev FCM Service - updateNotificationBadge called. tokens: {}, badge: {}", deviceTokens, badge);
    }

    @Override
    public void sendSilentMessage(String deviceToken, FcmSilentType type) {
        log.debug("Dev FCM Service - sendSilentMessage called. token: {}, type: {}", deviceToken, type);
    }
}
