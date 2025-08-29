package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.exception.NotificationSendException;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
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

    @Qualifier("notificationExecutor")
    private final ExecutorService notificationExecutor;

    @Override
    public void sendNotification(List<String> deviceTokenList, NotificationSend notificationSend) {
        // iOS용 APNs 설정
        ApnsConfig apnsConfig = getApnsConfig(notificationSend);
        MulticastMessage message = getMulticastMessage(deviceTokenList, notificationSend.title(),
                notificationSend.body(), apnsConfig);

        sendMessages(message);
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
                        .build())
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
    public void sendMessages(MulticastMessage message) {
        ApiFuture<BatchResponse> future = firebaseMessaging.sendEachForMulticastAsync(message);
        future.addListener(() -> listener(future), notificationExecutor);
    }

    private void listener(ApiFuture<BatchResponse> future) {
        try {
            BatchResponse batchResponse = future.get();
            if (batchResponse.getFailureCount() > 0) {
                loggedFailure(batchResponse);
                throw new NotificationSendException();
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

    private void loggedFailure(BatchResponse batchResponse) {
        batchResponse.getResponses()
                .stream()
                .filter(response -> !response.isSuccessful())
                .forEach(response -> log.error("Error sending FCM message: {}",
                        response.getException().getMessage()));
    }
}
