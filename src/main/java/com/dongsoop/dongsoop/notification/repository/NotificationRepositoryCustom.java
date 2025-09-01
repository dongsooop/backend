package com.dongsoop.dongsoop.notification.repository;

import com.dongsoop.dongsoop.notification.dto.NotificationList;
import com.dongsoop.dongsoop.notification.dto.NotificationUnread;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface NotificationRepositoryCustom {

    List<NotificationList> getMemberNotifications(Long memberId, Pageable pageable);

    long findUnreadCountByMemberId(Long memberId);

    List<NotificationUnread> findUnreadCountByMemberIds(Collection<Long> memberIds);

    Optional<MemberNotification> findByMemberIdAndNotificationId(Long memberId, Long notificationId);
}
