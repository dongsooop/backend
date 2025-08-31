package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.exception.NotificationSendException;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import io.netty.handler.timeout.TimeoutException;
import java.util.List;
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
    public void sendNotification(List<String> deviceTokenList, NotificationSend notificationSend) {
        // iOS용 APNs 설정
        ApnsConfig apnsConfig = getApnsConfig(notificationSend);
        MulticastMessage message = getMulticastMessage(deviceTokenList, notificationSend.title(),
                notificationSend.body(), apnsConfig);

        sendMessages(message, deviceTokenList);
    }

    @Override
    public ApnsConfig getApnsConfig(NotificationSend notificationSend) {
        return ApnsConfig.builder()
                .putCustomData("type", notificationSend.type().toString())
                .putCustomData("value", notificationSend.value())
                .putCustomData("id", String.valueOf(notificationSend.id()))
                .setAps(Aps.builder()
                        .setAlert(ApsAlert.builder()
                                .setTitle(notificationSend.title())
                                .setBody(notificationSend.body())
                                .build())
                        .setSound("default")
                        .setBadge(1)
                        .build())
                .build();
    }

    @Override
    public Aps getAps(String title, String body, int badge) {
        return Aps.builder()
                .setAlert(ApsAlert.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setSound("default")
                .setBadge(badge)
                .build();
    }

    private MulticastMessage getMulticastMessage(
            List<String> deviceTokenList,
            String title,
            String body,
            ApnsConfig apnsConfig) {
        return MulticastMessage.builder()
                .addAllTokens(deviceTokenList)
                .setApnsConfig(apnsConfig)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .build();
    }

    @Override
    public void sendMessages(MulticastMessage message, List<String> tokens) {
        ApiFuture<BatchResponse> future = firebaseMessaging.sendEachForMulticastAsync(message);
        future.addListener(() -> listener(future, tokens), notificationExecutor);
    }

    private void listener(ApiFuture<BatchResponse> future, List<String> tokens) {
        try {
            BatchResponse batchResponse = future.get();
            if (batchResponse.getFailureCount() > 0) {
                handleFailure(batchResponse, tokens);
            }
            log.info("Successfully sent messages: {}", batchResponse.getSuccessCount());

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
            return;
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

            // 무효한 토큰 확인
            if (isInvalidToken(exception)) {
                String invalidToken = tokens.get(i); // 토큰 리스트와 매핑
                memberDeviceService.deleteByToken(invalidToken);
                log.warn("Invalid FCM token removed: {}", invalidToken);
            } else {
                log.error("Error sending FCM message: {}", exception.getMessage());
            }
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException exception) {
        MessagingErrorCode messagingErrorCode = exception.getMessagingErrorCode();
        if (messagingErrorCode == null) {
            return false;
        }

        boolean isUnregistered = messagingErrorCode.equals(MessagingErrorCode.UNREGISTERED);
        boolean isInvalidArgument = messagingErrorCode.equals(MessagingErrorCode.INVALID_ARGUMENT);

        return isUnregistered || isInvalidArgument;
    }
}
