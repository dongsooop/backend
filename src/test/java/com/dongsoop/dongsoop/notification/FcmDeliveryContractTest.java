package com.dongsoop.dongsoop.notification;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.constant.FcmSilentType;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.exception.NotificationSendException;
import com.dongsoop.dongsoop.notification.exception.ResponseSizeUnmatchedToTokenSizeException;
import com.dongsoop.dongsoop.notification.service.FCMServiceImpl;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FcmDeliveryContractTest {
    @Mock FirebaseMessaging firebase;
    @Mock MemberDeviceService devices;
    @Mock ExecutorService executor;
    @Mock ApiFuture<BatchResponse> batchFuture;
    @Mock ApiFuture<String> singleFuture;
    FCMServiceImpl service;
    private final List<String> tokens = List.of("token-a", "token-b");
    private final NotificationSend payload = new NotificationSend(9L, "title", "body", NotificationType.NOTICE, "42");

    @BeforeEach
    void setup() { service = new FCMServiceImpl(firebase, devices, executor); }

    @AfterEach
    void clearInterrupt() { Thread.interrupted(); }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1, 0, 3})
    void notificationKeepsPlatformPayloadAndOptionalBadge(Integer badge) {
        when(firebase.sendEachForMulticastAsync(any())).thenReturn(batchFuture);
        service.sendNotification(tokens, payload, badge);
        ArgumentCaptor<MulticastMessage> captured = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebase).sendEachForMulticastAsync(captured.capture());
        Object message = captured.getValue();
        assertThat(field(message, "tokens")).isEqualTo(tokens);
        Object notification = field(message, "notification");
        assertThat(field(notification, "title")).isEqualTo("title");
        assertThat(field(notification, "body")).isEqualTo("body");
        Map<?, ?> apns = map(field(message, "apnsConfig"), "payload");
        assertThat(apns.get("type")).isEqualTo("NOTICE");
        assertThat(apns.get("value")).isEqualTo("42");
        assertThat(apns.get("id")).isEqualTo("9");
        Map<?, ?> aps = (Map<?, ?>) apns.get("aps");
        Map<?, ?> android = map(field(message, "androidConfig"), "data");
        assertThat(android.get("type")).isEqualTo("NOTICE");
        assertThat(android.get("id")).isEqualTo("9");
        assertThat(android.get("value")).isEqualTo("42");
        if (badge == null || badge < 0) {
            assertThat(aps.containsKey("badge")).isFalse();
            assertThat(android.containsKey("badge")).isFalse();
            return;
        }
        assertThat(aps.get("badge")).isEqualTo(badge);
        assertThat(android.get("badge")).isEqualTo(badge.toString());
    }

    @Test
    void topicAndSilentMessagesKeepDestinationAndVisibility() {
        when(firebase.sendAsync(any())).thenReturn(singleFuture);
        service.sendToTopic("department", payload);
        service.sendSilentMessage("token-a", FcmSilentType.FORCE_LOGOUT);
        ArgumentCaptor<Message> captured = ArgumentCaptor.forClass(Message.class);
        verify(firebase, times(2)).sendAsync(captured.capture());
        assertThat(field(captured.getAllValues().get(0), "topic")).isEqualTo("department");
        Object silent = captured.getAllValues().get(1);
        assertThat(field(silent, "token")).isEqualTo("token-a");
        assertThat(field(silent, "notification")).isNull();
        assertThat(map(silent, "data").get("type")).isEqualTo("FORCE_LOGOUT");
        assertThat(field(field(silent, "androidConfig"), "priority")).isEqualTo("high");
        Map<?, ?> aps = (Map<?, ?>) map(field(silent, "apnsConfig"), "payload").get("aps");
        assertThat(aps.get("content-available")).isEqualTo(1);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void missingSilentTokenIsRejected(String token) {
        assertThatIllegalArgumentException().isThrownBy(() -> service.sendSilentMessage(token, FcmSilentType.FORCE_LOGOUT));
        verifyNoInteractions(firebase);
    }

    @Test
    void badgeOnlyUpdateSkipsMissingTargetsAndKeepsSilentPayload() {
        service.updateNotificationBadge(null, 2);
        service.updateNotificationBadge(List.of(), 2);
        verifyNoInteractions(firebase);
        when(firebase.sendEachForMulticastAsync(any())).thenReturn(batchFuture);
        service.updateNotificationBadge(tokens, 2);
        ArgumentCaptor<MulticastMessage> captured = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebase).sendEachForMulticastAsync(captured.capture());
        assertThat(field(captured.getValue(), "notification")).isNull();
        assertThat(map(captured.getValue(), "data").get("badge")).isEqualTo("2");
        Map<?, ?> aps = (Map<?, ?>) map(field(captured.getValue(), "apnsConfig"), "payload").get("aps");
        assertThat(aps.get("badge")).isEqualTo(2);
        assertThat(aps.get("content-available")).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(MessagingErrorCode.class)
    void failedResponseOnlyUnbindsPermanentInvalidTokens(MessagingErrorCode code) throws Exception {
        FirebaseMessagingException error = mock(FirebaseMessagingException.class);
        when(error.getMessagingErrorCode()).thenReturn(code);
        SendResponse failed = mock(SendResponse.class);
        when(failed.getException()).thenReturn(error);
        SendResponse success = mock(SendResponse.class);
        when(success.isSuccessful()).thenReturn(true);
        BatchResponse batch = mock(BatchResponse.class);
        when(batch.getFailureCount()).thenReturn(1);
        when(batch.getResponses()).thenReturn(List.of(success, failed));
        when(batchFuture.get()).thenReturn(batch);
        Runnable listener = prepareBatchListener();
        assertThatThrownBy(listener::run).isInstanceOf(NotificationSendException.class);
        if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
            verify(devices).unbindByToken("token-b");
            return;
        }
        verifyNoInteractions(devices);
    }

    @Test
    void responseCountMismatchCannotUnbindWrongToken() throws Exception {
        BatchResponse batch = mock(BatchResponse.class);
        when(batch.getFailureCount()).thenReturn(1);
        when(batch.getResponses()).thenReturn(List.of());
        when(batchFuture.get()).thenReturn(batch);
        Runnable listener = prepareBatchListener();
        assertThatThrownBy(listener::run).isInstanceOf(ResponseSizeUnmatchedToTokenSizeException.class);
        verifyNoInteractions(devices);
    }

    @Test
    void successfulBatchAndSingleResponsesCompleteWithoutDeviceChanges() throws Exception {
        BatchResponse batch = mock(BatchResponse.class);
        when(batchFuture.get()).thenReturn(batch);
        prepareBatchListener().run();
        when(firebase.sendAsync(any())).thenReturn(singleFuture);
        when(singleFuture.get()).thenReturn("message-id");
        service.sendSilentMessage("token-a", FcmSilentType.ECLASS_RELINK);
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        verify(singleFuture).addListener(callback.capture(), same(executor));
        assertThatCode(callback.getValue()::run).doesNotThrowAnyException();
        verifyNoInteractions(devices);
    }

    @ParameterizedTest
    @EnumSource(FutureFailure.class)
    void futureErrorsBecomeDeliveryErrorsAndInterruptIsRestored(FutureFailure failure) throws Exception {
        when(batchFuture.get()).thenThrow(failure.error());
        Runnable listener = prepareBatchListener();
        assertThatThrownBy(listener::run).isInstanceOf(NotificationSendException.class);
        assertThat(Thread.currentThread().isInterrupted()).isEqualTo(failure == FutureFailure.INTERRUPTED);
        verifyNoInteractions(devices);
    }

    @Test
    void subscriptionErrorsAreTranslated() throws Exception {
        FirebaseMessagingException error = mock(FirebaseMessagingException.class);
        when(firebase.subscribeToTopic(tokens, "topic")).thenThrow(error);
        when(firebase.unsubscribeFromTopic(tokens, "topic")).thenThrow(error);
        assertThatThrownBy(() -> service.subscribeTopic(tokens, "topic")).isInstanceOf(NotificationSendException.class);
        assertThatThrownBy(() -> service.unsubscribeTopic(tokens, "topic")).isInstanceOf(NotificationSendException.class);
    }

    private Runnable prepareBatchListener() {
        when(firebase.sendEachForMulticastAsync(any())).thenReturn(batchFuture);
        service.sendNotification(tokens, payload, 1);
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        verify(batchFuture).addListener(callback.capture(), same(executor));
        return callback.getValue();
    }

    private Object field(Object value, String name) { return ReflectionTestUtils.getField(value, name); }
    private Map<?, ?> map(Object value, String name) { return (Map<?, ?>) field(value, name); }

    private enum FutureFailure {
        INTERRUPTED, EXECUTION, CANCELLED, RUNTIME;
        Exception error() {
            return switch (this) {
                case INTERRUPTED -> new InterruptedException("test");
                case EXECUTION -> new ExecutionException(new IllegalStateException("test"));
                case CANCELLED -> new CancellationException("test");
                case RUNTIME -> new IllegalStateException("test");
            };
        }
    }
}
