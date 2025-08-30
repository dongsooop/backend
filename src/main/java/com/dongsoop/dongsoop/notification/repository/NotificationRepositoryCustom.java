package com.dongsoop.dongsoop.notification.repository;

import com.dongsoop.dongsoop.notification.dto.NotificationList;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface NotificationRepositoryCustom {

    List<NotificationList> getMemberNotifications(Long memberId, Pageable pageable);

    Long findUnreadCountByMemberId(Long memberId);

    Optional<MemberNotification> findByMemberIdAndNotificationId(Long memberId, Long notificationId);
}
