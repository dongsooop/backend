package com.dongsoop.dongsoop.timetable.notification;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.timetable.dto.TodayTimetable;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimetableNotificationImpl implements TimetableNotification {

    private final static String TITLE_FORMAT = "[시간표 알림] 오늘 %d개의 수업이 있습니다";
    private final static Long NON_SAVE_NOTIFICATION_ID = -1L;

    private final NotificationSendService notificationSendService;

    @Override
    public void send(Long memberId, List<TodayTimetable> timetables, Map<Long, List<String>> deviceByMember) {
        String title = String.format(TITLE_FORMAT, timetables.size());
        String body = getTimetableBody(timetables);

        List<String> devices = deviceByMember.getOrDefault(memberId, List.of());

        // 알림 저장용 DTO
        NotificationSend notificationSend = new NotificationSend(NON_SAVE_NOTIFICATION_ID, title, body,
                NotificationType.TIMETABLE, null);

        // 알림 내용 전송
        notificationSendService.send(devices, notificationSend);
    }

    /**
     * 시간표 본문 생성
     *
     * @param timetables 시간표 리스트
     * @return 시간표 본문
     */
    private String getTimetableBody(List<TodayTimetable> timetables) {
        StringBuilder stringBuilder = new StringBuilder();
        timetables.forEach(timetable -> {
            String timeFormat = timetable.startAt()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));
            stringBuilder.append(timeFormat)
                    .append(" ")
                    .append(timetable.name())
                    .append("\n");
        });

        return stringBuilder.toString()
                .trim();
    }
}
