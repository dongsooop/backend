package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.exception.NotificationSendException;
import com.dongsoop.dongsoop.notification.exception.ResponseSizeUnmatchedToTokenSizeException;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import io.netty.handler.timeout.TimeoutException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class FCMServiceImpl implements FCMService {

    private final FirebaseMessaging firebaseMessaging;
    private final MemberDeviceService memberDeviceService;

    @Qualifier("notificationExecutor")
    private final ExecutorService notificationExecutor;

    @Override
    public void subscribeTopic(List<String> token, String topic) {
        try {
            firebaseMessaging.subscribeToTopic(token, topic);
        } catch (FirebaseMessagingException e) {
            log.error("Error subscribing to topic {}: {}", topic, e.getMessage());
            throw new NotificationSendException(e);
        }
    }

    @Override
    public void unsubscribeTopic(List<String> token, String topic) {
        try {
            firebaseMessaging.unsubscribeFromTopic(token, topic);
        } catch (FirebaseMessagingException e) {
            log.error("Error unsubscribing from topic {}: {}", topic, e.getMessage());
            throw new NotificationSendException(e);
        } catch (Exception e) {
            log.warn("Failed to unsubscribe device from anonymous topic", e);
        }
    }

    @Override
    public void sendNotification(List<String> deviceTokenList, NotificationSend notificationSend, Integer badge) {
        // iOS용 APNs 설정
        ApnsConfig apnsConfig = getApnsConfig(notificationSend, badge);

        // Android용 설정
        AndroidConfig androidConfig = getAndroidConfig(notificationSend, badge);

        MulticastMessage message = getMulticastMessage(deviceTokenList, notificationSend.title(),
                notificationSend.body(), apnsConfig, androidConfig);

        sendMessages(message, deviceTokenList);
    }

    @Override
    public void sendToTopic(String topic, NotificationSend notificationSend) {
        ApnsConfig apnsConfig = getApnsConfig(notificationSend, null);
        AndroidConfig androidConfig = getAndroidConfig(notificationSend, null);

        Notification notification = getNotification(notificationSend.title(), notificationSend.body());

        Message message = Message.builder()
                .setTopic(topic)
                .setNotification(notification)
                .setApnsConfig(apnsConfig)
                .setAndroidConfig(androidConfig)
                .build();

        sendMessage(message);
    }

    public ApnsConfig getApnsConfig(NotificationSend notificationSend, Integer badge) {
        Aps aps = getAps(notificationSend.title(), notificationSend.body(), badge);

        return ApnsConfig.builder()
                .putCustomData("type", notificationSend.type().toString())
                .putCustomData("value", notificationSend.value())
                .putCustomData("id", String.valueOf(notificationSend.id()))
                .setAps(aps)
                .build();
    }

    private Notification getNotification(String title, String body) {
        return Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();
    }

    private AndroidConfig getAndroidConfig(NotificationSend notificationSend, Integer badge) {
        AndroidConfig.Builder builder = AndroidConfig.builder()
                .setNotification(AndroidNotification.builder()
                        .setChannelId(notificationSend.type().name())
                        .build())
                .putAllData(Map.of(
                        "type", notificationSend.type().name(),
                        "value", notificationSend.value(),
                        "id", String.valueOf(notificationSend.id())
                ));

        if (isInvalidBadge(badge)) {
            return builder.build();
        }

        return builder.putData("badge", String.valueOf(badge))
                .build();
    }

    private Aps getAps(String title, String body, Integer badge) {
        Aps.Builder builder = Aps.builder()
                .setAlert(ApsAlert.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setSound("default");

        if (isInvalidBadge(badge)) {
            return builder.build();
        }

        return builder.setBadge(badge)
                .build();
    }

    private boolean isInvalidBadge(Integer badge) {
        return badge == null || badge < 0;
    }

    private MulticastMessage getMulticastMessage(
            List<String> deviceTokenList,
            String title,
            String body,
            ApnsConfig apnsConfig,
            AndroidConfig androidConfig) {
        Notification notification = getNotification(title, body);

        return MulticastMessage.builder()
                .addAllTokens(deviceTokenList)
                .setApnsConfig(apnsConfig)
                .setAndroidConfig(androidConfig)
                .setNotification(notification)
                .build();
    }

    @Override
    public void sendMessages(MulticastMessage message, List<String> tokens) {
        ApiFuture<BatchResponse> future = firebaseMessaging.sendEachForMulticastAsync(message);
        future.addListener(() -> listener(future, tokens), notificationExecutor);
    }

    @Override
    public void sendMessage(Message message) {
        ApiFuture<String> future = firebaseMessaging.sendAsync(message);
        future.addListener(() -> listener(future), notificationExecutor);
    }

    private void listener(ApiFuture<String> future) {
        String response = getResponse(future);

        log.info("Successfully sent messages: {}", response);
    }

    private void listener(ApiFuture<BatchResponse> future, List<String> tokens) {
        BatchResponse response = getResponse(future);

        if (response.getFailureCount() > 0) {
            handleFailure(response, tokens);
            throw new NotificationSendException();
        }

        log.info("Successfully sent messages: {}", response.getSuccessCount());
    }

    private <T> T getResponse(ApiFuture<T> future) {
        try {
            return future.get();

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while sending FCM messages", ie);
            throw new NotificationSendException(ie);

        } catch (ExecutionException ee) {
            log.error("Failed to send FCM messages due to execution error", ee);
            throw new NotificationSendException(ee);

        } catch (TimeoutException te) {
            log.error("Timeout waiting for FCM response", te);
            throw new NotificationSendException(te);

        } catch (CancellationException ce) {
            log.error("FCM send operation was cancelled", ce);
            throw new NotificationSendException(ce);

        } catch (RuntimeException re) {
            log.error("Unexpected error during FCM send", re);
            throw new NotificationSendException(re);
        }
    }

    private void handleFailure(BatchResponse batchResponse, List<String> tokens) {
        List<SendResponse> responses = batchResponse.getResponses();
        if (tokens.size() != responses.size()) {
            log.warn("Token list size does not match response size: tokens={}, responses={}",
                    tokens.size(), responses.size());
            throw new ResponseSizeUnmatchedToTokenSizeException(responses.size(), tokens.size());
        }

        for (int i = 0; i < responses.size(); i++) {
            SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                continue;
            }

            FirebaseMessagingException exception = response.getException();
            if (exception == null) {
                continue;
            }

            // 만료된 토큰 확인
            if (isValidToken(exception)) {
                String invalidToken = tokens.get(i);
                memberDeviceService.deleteByToken(invalidToken);
                log.warn("Invalid FCM token removed: {}", invalidToken);

                continue;
            }

            log.error("Error sending FCM message: {}", exception.getMessage());
        }
    }

    private boolean isValidToken(FirebaseMessagingException exception) {
        MessagingErrorCode messagingErrorCode = exception.getMessagingErrorCode();
        if (messagingErrorCode == null) {
            return false;
        }

        boolean isUnregistered = messagingErrorCode.equals(MessagingErrorCode.UNREGISTERED);
        boolean isInvalidArgument = messagingErrorCode.equals(MessagingErrorCode.INVALID_ARGUMENT);

        return isUnregistered || isInvalidArgument;
    }

    public void updateNotificationBadge(List<String> deviceTokens, int badge) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.warn(
                    "updateNotificationBadge called with null or empty deviceTokens. No FCM operation will be performed.");
            return;
        }

        ApnsConfig apnsConfig = ApnsConfig.builder()
                .setAps(Aps.builder()
                        .setBadge(badge)
                        .setContentAvailable(true)
                        .build())
                .putHeader("apns-priority", "10")
                .build();

        AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.NORMAL)
                .build();

        MulticastMessage messages = MulticastMessage.builder()
                .addAllTokens(deviceTokens)
                .setApnsConfig(apnsConfig)
                .setAndroidConfig(androidConfig)
                .putData("badge", String.valueOf(badge))
                .build();

        sendMessages(messages, deviceTokens);
    }
}
