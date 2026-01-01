package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.dto.NotificationUnread;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSendServiceImpl implements NotificationSendService {

    private final MemberDeviceService memberDeviceService;
    private final NotificationRepository notificationRepository;
    private final FCMService fcmService;

    /**
     * 복합 알림 전송
     *
     * @param memberNotificationList 저장된 알림 리스트
     */
    @Override
    @Transactional(readOnly = true)
    public void sendAll(List<MemberNotification> memberNotificationList, NotificationType notificationType) {
        // 알림을 보낼 대상 회원들
        List<Long> memberIds = memberNotificationList.stream()
                .map(notification -> notification.getId().getMember().getId())
                .distinct()
                .toList();

        // 저장된 알림 -> Map 변환 (key: 알림 상세, value: 알림 대상 회원 ID 리스트)
        Map<NotificationDetails, List<Long>> memberByNotification = listToMap(
                memberNotificationList);

        // 발송 전체 대상의 디바이스 토큰
        MemberDeviceFindCondition condition = new MemberDeviceFindCondition(memberIds, notificationType);
        Map<Long, List<String>> memberIdDevices = memberDeviceService.getDeviceByMember(condition);

        // 발송 전체 대상의 회원별 읽지 않은 알림 개수
        List<NotificationUnread> unreadCount = notificationRepository.findUnreadCountByMemberIds(memberIds);
        Map<Long, Integer> unreadCountByMember = unreadCount.stream()
                .collect(Collectors.toMap(NotificationUnread::getMemberId, NotificationUnread::getUnreadCount));

        memberByNotification.forEach((details, memberIdList) -> {
            NotificationSend notificationSend = getNotificationSendByDetails(details);

            memberIdList.forEach(memberId -> {
                List<String> devices = memberIdDevices.getOrDefault(memberId, Collections.emptyList());
                if (devices.isEmpty()) {
                    return;
                }

                Integer badge = unreadCountByMember.getOrDefault(memberId, 0);
                fcmService.sendNotification(devices, notificationSend, badge);
            });
        });
    }

    private Map<NotificationDetails, List<Long>> listToMap(List<MemberNotification> memberNotificationList) {
        return memberNotificationList.stream().collect(Collectors.groupingBy(
                notification -> notification.getId().getDetails(),
                Collectors.mapping(
                        notification -> notification.getId().getMember().getId(),
                        Collectors.toList()
                )));
    }

    @Override
    public void send(String topic, NotificationSend notificationSend) {
        fcmService.sendToTopic(topic, notificationSend);
    }

    @Override
    public void send(List<String> deviceTokenList, NotificationSend notificationSend) {
        fcmService.sendNotification(deviceTokenList, notificationSend, null);
    }

    /**
     * 단일 알림 전송
     *
     * @param memberNotification 저장된 알림
     */
    @Override
    @Transactional(readOnly = true)
    public void send(MemberNotification memberNotification) {
        // 알림을 보낼 대상 회원
        Long memberId = memberNotification.getId()
                .getMember()
                .getId();

        List<String> deviceByMemberId = memberDeviceService.getDeviceByMemberId(memberId);
        if (deviceByMemberId.isEmpty()) {
            return;
        }

        NotificationDetails details = memberNotification.getId()
                .getDetails();

        long unreadCount = notificationRepository.findUnreadCountByMemberId(memberId);

        NotificationSend notificationSend = getNotificationSendByDetails(details);
        fcmService.sendNotification(deviceByMemberId, notificationSend, Math.toIntExact(unreadCount));
    }

    private NotificationSend getNotificationSendByDetails(NotificationDetails details) {
        return new NotificationSend(
                details.getId(),
                details.getTitle(),
                details.getBody(),
                details.getType(),
                details.getValue()
        );
    }
}
