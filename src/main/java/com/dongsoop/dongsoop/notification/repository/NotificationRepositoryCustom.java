package com.dongsoop.dongsoop.notification.repository;

import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface NotificationRepositoryCustom {

    List<NotificationDetails> getMemberNotifications(Long memberId, Pageable pageable);

    Optional<MemberNotification> findByMemberIdAndNotificationId(Long memberId, Long notificationId);
}
