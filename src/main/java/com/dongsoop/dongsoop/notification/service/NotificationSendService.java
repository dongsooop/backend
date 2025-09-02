package com.dongsoop.dongsoop.notification.service;

import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import java.util.List;

public interface NotificationSendService {

    // 단일 회원에게 알림 전송
    void send(MemberNotification memberNotification);

    // 알림을 저장하지 않은 회원들에게 알림 전송
    void send(List<String> deviceTokenList, NotificationSend notificationSend);

    // 토픽으로 알림 전송
    void send(String topic, NotificationSend notificationSend);

    // 여러 회원에게 복합 알림 전송
    void sendAll(List<MemberNotification> memberNotificationList);
}
