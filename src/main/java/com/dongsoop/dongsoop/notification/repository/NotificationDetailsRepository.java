package com.dongsoop.dongsoop.notification.repository;

import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationDetailsRepository extends JpaRepository<NotificationDetails, Long> {
}
