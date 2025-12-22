package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.dto.EventNotification;
import com.dongsoop.dongsoop.notification.dto.NotificationList;
import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.exception.NotificationNotFoundException;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationSendService notificationSendService;
    private final NotificationRepository notificationRepository;
    private final FCMService fcmService;
    private final MemberDeviceService memberDeviceService;

    @Value("${notification.topic.event}")
    private String eventTopic;

    @Value("${notification.non-save-id}")
    private Long nonSaveNotificationId;

    @Override
    public NotificationOverview getNotifications(Pageable pageable, Long memberId) {
        long unreadCount = notificationRepository.findUnreadCountByMemberId(memberId);
        List<NotificationList> notificationLists = notificationRepository.getMemberNotifications(memberId, pageable);

        return new NotificationOverview(notificationLists, unreadCount);
    }

    @Override
    @Transactional
    public void deleteMemberNotification(Long notificationId, Long memberId) {
        MemberNotification memberNotification = notificationRepository.findByMemberIdAndNotificationId(memberId,
                        notificationId)
                .orElseThrow(NotificationNotFoundException::new);

        memberNotification.delete();
    }

    @Override
    @Transactional
    public void read(Long notificationId, Long memberId) {
        MemberNotification memberNotification = notificationRepository.findByMemberIdAndNotificationId(memberId,
                        notificationId)
                .orElseThrow(NotificationNotFoundException::new);

        memberNotification.read();

        int unreadCountByMemberId = notificationRepository.findUnreadCountByMemberId(memberId);
        List<String> devices = memberDeviceService.getDeviceByMemberId(memberId);

        fcmService.updateNotificationBadge(devices, unreadCountByMemberId);
    }

    /**
     * 요청 회원의 모든 알림 읽음 처리 및 뱃지 상태 업데이트
     *
     * @param memberId 요청 회원 ID
     */
    @Override
    @Transactional
    public void readAll(Long memberId) {
        notificationRepository.updateAllAsRead(memberId);

        List<String> devices = memberDeviceService.getDeviceByMemberId(memberId);
        int unreadCountByMemberId = notificationRepository.findUnreadCountByMemberId(memberId);
        fcmService.updateNotificationBadge(devices, unreadCountByMemberId);
    }

    public void sendEventNotification(EventNotification eventNotification) {
        NotificationSend notificationSend = new NotificationSend(
                nonSaveNotificationId,
                eventNotification.title(),
                eventNotification.body(),
                eventNotification.type(),
                ""
        );

        notificationSendService.send(eventTopic, notificationSend);

    }
}
