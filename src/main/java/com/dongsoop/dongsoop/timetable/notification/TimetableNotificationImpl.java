package com.dongsoop.dongsoop.timetable.notification;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimetableNotificationImpl implements TimetableNotification {

    private final static Long NON_SAVE_NOTIFICATION_ID = -1L;

    private final NotificationSendService notificationSendService;

    @Override
    public void send(String title, String body, List<String> devices) {
        // 알림 저장용 DTO
        NotificationSend notificationSend = new NotificationSend(NON_SAVE_NOTIFICATION_ID, title, body,
                NotificationType.TIMETABLE, "");

        // 알림 내용 전송
        notificationSendService.send(devices, notificationSend);
    }
}
