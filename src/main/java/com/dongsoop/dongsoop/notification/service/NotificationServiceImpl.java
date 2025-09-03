package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.dto.NotificationList;
import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.exception.NotificationNotFoundException;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberService memberService;
    private final FCMService fcmService;
    private final MemberDeviceService memberDeviceService;

    @Override
    public NotificationOverview getNotifications(Pageable pageable) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        long unreadCount = notificationRepository.findUnreadCountByMemberId(requesterId);
        List<NotificationList> notificationLists = notificationRepository.getMemberNotifications(requesterId,
                pageable);

        return new NotificationOverview(notificationLists, unreadCount);
    }

    @Override
    @Transactional
    public void deleteMemberNotification(Long id) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        MemberNotification memberNotification = notificationRepository.findByMemberIdAndNotificationId(requesterId, id)
                .orElseThrow(NotificationNotFoundException::new);

        memberNotification.delete();
    }

    @Override
    @Transactional
    public void read(Long id) {
        Long requesterId = memberService.getMemberIdByAuthentication();

        MemberNotification memberNotification = notificationRepository.findByMemberIdAndNotificationId(requesterId, id)
                .orElseThrow(NotificationNotFoundException::new);

        memberNotification.read();

        long unreadCountByMemberId = notificationRepository.findUnreadCountByMemberId(requesterId);
        List<String> devices = memberDeviceService.getDeviceByMemberId(requesterId);

        fcmService.updateNotificationBadge(devices, (int) unreadCountByMemberId);
    }

    @Override
    public void readAll() {
        Long requesterId = memberService.getMemberIdByAuthentication();

        notificationRepository.updateAllAsRead(requesterId);
    }

    @Override
    @Async
    public void sendUpdatedBadge() {
        Long requesterId = memberService.getMemberIdByAuthentication();

        List<String> devices = memberDeviceService.getDeviceByMemberId(requesterId);
        fcmService.updateNotificationBadge(devices, 0);
    }
}
