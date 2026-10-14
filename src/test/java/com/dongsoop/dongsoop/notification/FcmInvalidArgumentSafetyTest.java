package com.dongsoop.dongsoop.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.exception.NotificationSendException;
import com.dongsoop.dongsoop.notification.service.FCMServiceImpl;
import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FcmInvalidArgumentSafetyTest {
    @Mock FirebaseMessaging firebase;
    @Mock MemberDeviceService devices;
    @Mock ExecutorService executor;
    @Mock ApiFuture<BatchResponse> future;

    @ParameterizedTest
    @EnumSource(MessagingErrorCode.class)
    void onlyConfirmedUnregisteredTokenIsUnbound(MessagingErrorCode errorCode) throws Exception {
        FirebaseMessagingException error = mock(FirebaseMessagingException.class);
        when(error.getMessagingErrorCode()).thenReturn(errorCode);
        SendResponse response = mock(SendResponse.class);
        when(response.getException()).thenReturn(error);
        BatchResponse batch = mock(BatchResponse.class);
        when(batch.getFailureCount()).thenReturn(1);
        when(batch.getResponses()).thenReturn(List.of(response));
        when(future.get()).thenReturn(batch);
        when(firebase.sendEachForMulticastAsync(any())).thenReturn(future);
        var service = new FCMServiceImpl(firebase, devices, executor);
        service.sendMessages(MulticastMessage.builder().addToken("retained-token").build(), List.of("retained-token"));
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        verify(future).addListener(callback.capture(), same(executor));
        assertThatThrownBy(callback.getValue()::run).isInstanceOf(NotificationSendException.class);
        if (errorCode == MessagingErrorCode.UNREGISTERED) {
            verify(devices).unbindByToken("retained-token");
            return;
        }
        verifyNoInteractions(devices);
    }
}
