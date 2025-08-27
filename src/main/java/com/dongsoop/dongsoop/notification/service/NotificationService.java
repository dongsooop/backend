package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import java.util.List;

public interface NotificationService {

    void save(List<MemberDeviceDto> memberDeviceDtoList, String title, String body, NotificationType type,
              String value);
}
