package com.dongsoop.dongsoop.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.service.FCMService;
import com.dongsoop.dongsoop.notification.service.NotificationBadgeService;
import com.dongsoop.dongsoop.notification.service.NotificationSendServiceImpl;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {
  @Mock MemberDeviceService devices;
  @Mock NotificationBadgeService badges;
  @Mock FCMService fcm;
  @InjectMocks NotificationSendServiceImpl service;

  @Test
  void singleNotificationIncludesDetailsAndUnreadBadge() {
    MemberNotification notification = notification(7L);
    when(devices.getDeviceByMemberId(7L)).thenReturn(List.of("device-a", "device-b"));
    when(badges.resolveByMemberId(7L)).thenReturn(3);
    service.send(notification);
    verify(fcm).sendNotification(List.of("device-a", "device-b"), payload(), 3);
  }

  @Test
  void absentDevicesSkipsBadgeAndSending() {
    when(devices.getDeviceByMemberId(7L)).thenReturn(List.of());
    service.send(notification(7L));
    verifyNoInteractions(badges, fcm);
  }

  @Test
  void batchResolvesMembersAndUsesDefaultBadgeForMissingEntry() {
    MemberDeviceFindCondition condition =
        new MemberDeviceFindCondition(List.of(7L, 8L), NotificationType.NOTICE);
    when(devices.getDeviceByMember(condition)).thenReturn(Map.of(7L, List.of("device-a")));
    when(badges.resolveByMemberIds(List.of(7L, 8L))).thenReturn(Map.of());
    service.sendAll(List.of(notification(7L), notification(8L)), NotificationType.NOTICE);
    verify(devices).getDeviceByMember(condition);
    verify(fcm).sendNotification(List.of("device-a"), payload(), 0);
  }

  @Test
  void explicitDevicesExcludeUnownedAndUnrelatedMembers() {
    Member member = Member.builder().id(7L).build();
    Member other = Member.builder().id(8L).build();
    List<MemberDevice> targets =
        List.of(
            MemberDevice.builder().member(member).deviceToken("selected").build(),
            MemberDevice.builder().deviceToken("unowned").build(),
            MemberDevice.builder().member(other).deviceToken("unrelated").build());
    when(badges.resolveByMemberIds(List.of(7L))).thenReturn(Map.of(7L, 2));
    service.sendAllToDevices(List.of(notification(7L)), targets);
    verify(fcm).sendNotification(List.of("selected"), payload(), 2);
    verifyNoInteractions(devices);
  }

  @Test
  void emptyBatchDoesNotDispatch() {
    when(devices.getDeviceByMember(any())).thenReturn(Map.of());
    when(badges.resolveByMemberIds(List.of())).thenReturn(Map.of());
    service.sendAll(List.of(), NotificationType.NOTICE);
    verifyNoInteractions(fcm);
  }

  @Test
  void topicAndDirectDevicesKeepPayloadWithoutMemberLookup() {
    NotificationSend payload = payload();
    service.send("department", payload);
    service.send(List.of("guest-device"), payload);
    verify(fcm).sendToTopic("department", payload);
    verify(fcm).sendNotification(List.of("guest-device"), payload, null);
    verifyNoInteractions(devices, badges);
  }

  @Test
  void fcmFailureIsPropagatedToCaller() {
    when(devices.getDeviceByMemberId(7L)).thenReturn(List.of("device-a"));
    when(badges.resolveByMemberId(7L)).thenReturn(3);
    doThrow(new IllegalStateException("unavailable"))
        .when(fcm)
        .sendNotification(List.of("device-a"), payload(), 3);
    MemberNotification notification = notification(7L);
    assertThatThrownBy(() -> service.send(notification)).isInstanceOf(IllegalStateException.class);
    verify(fcm, never()).sendToTopic(any(), any());
  }

  private MemberNotification notification(Long memberId) {
    NotificationDetails details =
        NotificationDetails.builder()
            .id(9L)
            .title("공지")
            .body("새 공지 내용")
            .type(NotificationType.NOTICE)
            .value("42")
            .build();
    return new MemberNotification(details, Member.builder().id(memberId).build());
  }

  private NotificationSend payload() {
    return new NotificationSend(9L, "공지", "새 공지 내용", NotificationType.NOTICE, "42");
  }
}
