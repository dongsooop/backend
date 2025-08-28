package com.dongsoop.dongsoop.notification.repository;

import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface NotificationRepositoryCustom {

    List<NotificationOverview> getMemberNotifications(Long memberId, Pageable pageable);

    Optional<MemberNotification> findByMemberIdAndNotificationId(Long memberId, Long notificationId);
}
