package com.dongsoop.dongsoop.notification;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.service.NotificationBadgeServiceImpl;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationBadgeServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private MemberDeviceService memberDeviceService;

    @Mock
    private FCMService fcmService;

    @InjectMocks
    private NotificationBadgeServiceImpl badgeService;

    @Test
    @DisplayName("회원의 모든 디바이스에 미읽음 개수를 배지로 전송한다")
    void pushesUnreadCountToMemberDevices() {
        List<String> devices = List.of("first-device", "second-device");
        when(memberDeviceService.getDeviceByMemberId(1L)).thenReturn(devices);
        when(notificationRepository.findUnreadCountByMemberId(1L)).thenReturn(7);

        badgeService.pushBadge(1L);

        verify(fcmService).updateNotificationBadge(devices, 7);
    }

    @Test
    @DisplayName("미읽음 알림이 없으면 0을 전송해 기존 배지를 지운다")
    void clearsBadgeWhenAllNotificationsAreRead() {
        List<String> devices = List.of("first-device");
        when(memberDeviceService.getDeviceByMemberId(1L)).thenReturn(devices);
        when(notificationRepository.findUnreadCountByMemberId(1L)).thenReturn(0);

        badgeService.pushBadge(1L);

        verify(fcmService).updateNotificationBadge(devices, 0);
    }

    @Test
    @DisplayName("회원의 디바이스가 없으면 배지를 전송하지 않는다")
    void doesNotSendBadgeWithoutDevices() {
        when(memberDeviceService.getDeviceByMemberId(1L)).thenReturn(List.of());

        badgeService.pushBadge(1L);

        verifyNoInteractions(fcmService);
    }
}
