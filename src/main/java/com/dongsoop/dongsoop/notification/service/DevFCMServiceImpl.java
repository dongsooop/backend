package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Profile("dev")
@RequiredArgsConstructor
public class DevFCMServiceImpl implements FCMService {

    @Override
    public void subscribeTopic(List<String> token, String topic) {
        log.info("Dev Profile - subscribeTopic called");
    }

    @Override
    public void unsubscribeTopic(List<String> token, String topic) {
        log.info("Dev Profile - unsubscribeTopic called");
    }

    @Override
    public void sendNotification(List<String> deviceTokenList, NotificationSend notificationSend, Integer badge) {
        log.info("Dev Profile - sendNotification called");
    }

    @Override
    public void sendToTopic(String topic, NotificationSend notificationSend) {
        log.info("Dev Profile - sendToTopic called");
    }

    public ApnsConfig getApnsConfig(NotificationSend notificationSend, Integer badge) {
        log.info("Dev Profile - getApnsConfig called");
        return null;
    }

    @Override
    public void sendMessages(MulticastMessage message, List<String> tokens) {
        log.info("Dev Profile - sendMessages called");
    }

    @Override
    public void sendMessage(Message message) {
        log.info("Dev Profile - sendMessage called");
    }

    public void updateNotificationBadge(List<String> deviceTokens, int badge) {
        log.info("Dev Profile - updateNotificationBadge called");
    }
}
