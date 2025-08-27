package com.dongsoop.dongsoop.notification.repository;

import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.MemberNotificationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<MemberNotification, MemberNotificationId> {
}
