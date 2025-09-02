package com.dongsoop.dongsoop.timetable.schedule;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import com.dongsoop.dongsoop.timetable.dto.TimetableNotificationDto;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TimetableSchedule {

    private final static Long NON_SAVE_NOTIFICATION_ID = -1L;

    private final TimetableRepository timetableRepository;
    private final NotificationSendService notificationSendService;

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void scheduled() {
        Year year = Year.now();
        int month = LocalDate.now().getMonthValue();
        DayOfWeek dayOfWeek = LocalDate.now()
                .getDayOfWeek();

        SemesterType semester = SemesterType.fromMonth(month);

        List<TimetableNotificationDto> allByYearAndSemester = timetableRepository.getTimetableNotificationDtoList(year,
                semester, dayOfWeek);
        allByYearAndSemester.stream()
                .collect(Collectors.groupingBy(TimetableNotificationDto::memberId))
                .forEach((memberId, timetables) -> {
                    String topic = String.valueOf(memberId);
                    String title = String.format("[시간표 알림] 오늘 %d개의 수업이 있습니다", timetables.size());
                    String body = getTimetableBody(timetables);

                    NotificationSend notificationSend = new NotificationSend(NON_SAVE_NOTIFICATION_ID, title, body,
                            NotificationType.TIMETABLE, null);
                    notificationSendService.send(topic, notificationSend);
                });

    }

    private String getTimetableBody(List<TimetableNotificationDto> timetables) {
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
