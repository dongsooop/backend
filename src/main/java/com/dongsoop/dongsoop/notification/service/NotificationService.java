package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void save(List<MemberDeviceDto> memberDeviceDtoList, String title, String body, NotificationType type,
              String value);

    List<NotificationOverview> getNotifications(Pageable pageable);

    void deleteMemberNotification(Long id);

    void read(Long id);
}
