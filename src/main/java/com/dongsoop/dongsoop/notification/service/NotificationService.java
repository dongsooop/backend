package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationOverview;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    List<MemberNotification> save(List<MemberDeviceDto> memberDeviceDtoList, String title, String body,
                                  NotificationType type,
                                  String value);

    void send(Map<NotificationDetails, List<Member>> memberByNotification);

    Map<NotificationDetails, List<Member>> listToMap(List<MemberNotification> memberNotificationList);

    NotificationOverview getNotifications(Pageable pageable);

    void deleteMemberNotification(Long id);

    void read(Long id);
}
