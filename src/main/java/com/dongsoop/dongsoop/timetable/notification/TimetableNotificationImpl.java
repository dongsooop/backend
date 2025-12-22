package com.dongsoop.dongsoop.timetable.notification;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimetableNotificationImpl implements TimetableNotification {

    private final NotificationSendService notificationSendService;


    @Value("${notification.non-save-id}")
    private Long nonSaveNotificationId;

    @Override
    public void send(String title, String body, List<String> devices) {
        // 알림 저장용 DTO
        NotificationSend notificationSend = new NotificationSend(nonSaveNotificationId, title, body,
                NotificationType.TIMETABLE, "");

        // 알림 내용 전송
        notificationSendService.send(devices, notificationSend);
    }
}
