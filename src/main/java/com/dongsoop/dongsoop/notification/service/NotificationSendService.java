package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import java.util.List;

public interface NotificationSendService {

    void send(MemberNotification memberNotification);

    void send(List<String> deviceTokenList, NotificationSend notificationSend);

    void sendAll(List<MemberNotification> memberNotificationList);
}
