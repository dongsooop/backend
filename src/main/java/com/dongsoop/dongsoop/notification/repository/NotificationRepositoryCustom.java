package com.dongsoop.dongsoop.notification.repository;

import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface NotificationRepositoryCustom {

    List<NotificationDetails> getMemberNotifications(Long memberId, Pageable pageable);
}
