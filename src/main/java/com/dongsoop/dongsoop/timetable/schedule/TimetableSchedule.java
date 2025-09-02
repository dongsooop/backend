package com.dongsoop.dongsoop.timetable.schedule;

import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.timetable.dto.TodayTimetable;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.notification.TimetableNotification;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TimetableSchedule {

    private final TimetableRepository timetableRepository;
    private final TimetableNotification timetableNotification;
    private final MemberDeviceService memberDeviceService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void scheduled() {
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
        Map<Long, List<String>> deviceByMember = memberDeviceService.getDeviceByMember(targetsIdList);

        timetableByMember.forEach(
                (memberId, timetables) -> timetableNotification.send(memberId, timetables, deviceByMember));
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
}
