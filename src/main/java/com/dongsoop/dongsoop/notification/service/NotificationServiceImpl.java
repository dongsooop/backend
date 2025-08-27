package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.repository.NotificationDetailsRepository;
import com.dongsoop.dongsoop.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    private final NotificationDetailsRepository notificationDetailsRepository;

    @Override
    public void save(List<MemberDeviceDto> memberDeviceDtoList, String title, String body, NotificationType type,
                     String value) {
        NotificationDetails details = NotificationDetails.builder()
                .title(title)
                .body(body)
                .type(type)
                .value(value)
                .build();

        notificationDetailsRepository.save(details);

        List<MemberNotification> memberNotificationList = memberDeviceDtoList.stream()
                .map((memberDevice) -> new MemberNotification(details, memberDevice.member()))
                .toList();

        notificationRepository.saveAll(memberNotificationList);
    }
}
