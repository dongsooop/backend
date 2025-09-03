package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    NotificationOverview getNotifications(Pageable pageable, Long memberId);

    void deleteMemberNotification(Long notificationId, Long memberId);

    void read(Long notificationId, Long memberId);

    void readAll(Long memberId);
}
