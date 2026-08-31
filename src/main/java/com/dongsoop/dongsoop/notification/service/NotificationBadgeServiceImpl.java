package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.dto.NotificationUnread;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationBadgeServiceImpl implements NotificationBadgeService {

    private final NotificationRepository notificationRepository;
    private final MemberDeviceService memberDeviceService;
    private final FCMService fcmService;

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Integer> resolveByMemberIds(Collection<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        return notificationRepository.findUnreadCountByMemberIds(memberIds).stream()
                .collect(Collectors.toMap(NotificationUnread::getMemberId, NotificationUnread::getUnreadCount));
    }

    @Override
    @Transactional(readOnly = true)
    public int resolveByMemberId(Long memberId) {
        return notificationRepository.findUnreadCountByMemberId(memberId);
    }

    @Override
    @Transactional(readOnly = true)
    public void pushBadge(Long memberId) {
        List<String> devices = memberDeviceService.getDeviceByMemberId(memberId);
        if (devices.isEmpty()) {
            return;
        }

        fcmService.updateNotificationBadge(devices, resolveByMemberId(memberId));
    }
}
