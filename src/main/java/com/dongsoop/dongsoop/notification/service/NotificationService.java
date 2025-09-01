package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    
    NotificationOverview getNotifications(Pageable pageable);

    void deleteMemberNotification(Long id);

    void read(Long id);
}
