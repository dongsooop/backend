package com.dongsoop.dongsoop.timetable.schedule;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.timetable.dto.TodayTimetable;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.notification.TimetableNotification;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class TimetableScheduler {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private static final String TITLE_FORMAT = "[시간표 알림] 오늘 %d개의 수업이 있습니다";

    private final TimetableRepository timetableRepository;
    private final TimetableNotification timetableNotification;
    private final MemberDeviceService memberDeviceService;

    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Seoul")
    public void scheduled() {
        log.info("Timetable Schedule started.");
        // 오늘의 시간표 조회
        List<TodayTimetable> allByYearAndSemester = getTodayTimetableList();

        // 회원별 시간표 그룹핑
        Map<Long, List<TodayTimetable>> timetableByMember = allByYearAndSemester.stream()
                .collect(Collectors.groupingBy(TodayTimetable::memberId));

        // 알림 대상 회원 리스트
        List<Long> targetsIdList = allByYearAndSemester.stream()
                .map(TodayTimetable::memberId)
                .toList();

        // 디바이스 있는 대상 회원 토큰 전체 조회
        MemberDeviceFindCondition condition = new MemberDeviceFindCondition(targetsIdList, NotificationType.TIMETABLE);
        Map<Long, List<String>> deviceByMember = memberDeviceService.getDeviceByMember(condition);

        timetableByMember.forEach(
                (memberId, timetables) -> {
                    String title = String.format(TITLE_FORMAT, timetables.size());
                    String body = getTimetableBody(timetables);

                    List<String> devices = deviceByMember.getOrDefault(memberId, List.of());
                    if (devices.isEmpty()) {
                        return;
                    }

                    timetableNotification.send(title, body, devices);
                });

        log.info("Timetable Schedule ended.");
    }

    /**
     * 오늘의 시간표 조회
     *
     * @return 오늘의 시간표 리스트
     */
    private List<TodayTimetable> getTodayTimetableList() {
        Year year = Year.now();
        int month = LocalDate.now().getMonthValue();
        DayOfWeek dayOfWeek = LocalDate.now()
                .getDayOfWeek();

        SemesterType semester = SemesterType.fromMonth(month);

        return timetableRepository.getTimetableNotificationDtoList(year, semester, dayOfWeek);
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
                    .format(dateTimeFormatter);

            stringBuilder
                    .append("- ")
                    .append(timeFormat)
                    .append(" ")
                    .append(timetable.name())
                    .append("\n");
        });

        return stringBuilder.toString()
                .trim();
    }
}
