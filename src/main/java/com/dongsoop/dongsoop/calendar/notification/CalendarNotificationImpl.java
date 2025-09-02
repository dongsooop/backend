package com.dongsoop.dongsoop.calendar.notification;


import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarNotificationImpl implements CalendarNotification {

    private static final Long NON_SAVE_NOTIFICATION_ID = -1L;
    private static final String TITLE_FORMAT = "[일정 알림] 오늘 %d개의 일정이 있습니다";

    private final NotificationSaveService notificationSaveService;
    private final NotificationSendService notificationSendService;

    @Value("${notification.topic.anonymous}")
    private String anonymousTopic;

    /**
     * 비회원 디바이스로 알림 전송
     *
     * @param officialCalendarSize 공통 일정 수
     * @param integratedBody       공통 일정 내용
     */
    @Override
    public void sendForAnonymous(int officialCalendarSize, String integratedBody) {
        if (officialCalendarSize == 0) {
            return;
        }

        NotificationSend notificationSend = new NotificationSend(
                NON_SAVE_NOTIFICATION_ID,
                String.format(TITLE_FORMAT, officialCalendarSize),
                integratedBody,
                NotificationType.CALENDAR,
                null);

        notificationSendService.send(anonymousTopic, notificationSend);
    }

    @Override
    @Transactional
    public void saveAndSendForMember(Member member, List<String> devices, String title, String body) {
        // 알림 저장 및 저장된 알림 ID 반환
        MemberNotification save = notificationSaveService.save(member, title, body, NotificationType.CALENDAR, null);
        Long notificationId = save.getId()
                .getDetails()
                .getId();

        // 알림 저장용 DTO
        NotificationSend notificationSend = new NotificationSend(notificationId, title, body,
                NotificationType.CALENDAR, null);

        // 알림 내용 전송
        notificationSendService.send(devices, notificationSend);
    }
}
