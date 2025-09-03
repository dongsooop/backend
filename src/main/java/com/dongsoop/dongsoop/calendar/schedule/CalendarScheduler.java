package com.dongsoop.dongsoop.calendar.schedule;

import com.dongsoop.dongsoop.calendar.dto.TodaySchedule;
import com.dongsoop.dongsoop.calendar.notification.CalendarNotification;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
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
public class CalendarScheduler {

    private static final String OFFICIAL_SCHEDULE_BODY_FORMAT = "- 학사 일정: %s 외 %d개";
    private static final String MEMBER_SCHEDULE_BODY_FORMAT = "- 개인 일정: %s 외 %d개";
    private static final String TITLE_FORMAT = "[일정 알림] 오늘 %d개의 일정이 있습니다";

    private final MemberDeviceService memberDeviceService;
    private final MemberScheduleRepository memberScheduleRepository;
    private final OfficialScheduleRepository officialScheduleRepository;
    private final CalendarNotification calendarNotification;

    @Scheduled(cron = "0 0 8 * * ?", zone = "Asia/Seoul")
    public void send() {
        log.info("Calendar Schedule started.");
        // 회원 일정 및 공통 일정 조회
        List<TodaySchedule> memberCalendarList = memberScheduleRepository.searchTodaySchedule();
        List<String> officialCalendarList = officialScheduleRepository.searchTodaySchedule();

        Map<Member, List<TodaySchedule>> membercalendarMap = memberCalendarsToMap(memberCalendarList);
        List<Long> targetIdList = memberCalendarList.stream()
                .map(todaySchedule -> todaySchedule.member().getId())
                .toList();

        Map<Long, List<String>> targetDevices = memberDeviceService.getDeviceByMember(targetIdList);

        // 공통 일정 수 및 내용
        int officialCalendarSize = officialCalendarList.size();
        String integratedBody = generateOfficialScheduleBody(officialCalendarList);

        // 회원 일정에 따라 알림 발송
        membercalendarMap.forEach((member, calendarList) -> {
            if (member == null) {
                return;
            }

            // 타겟 회원의 디바이스 목록
            List<String> devices = targetDevices.getOrDefault(member.getId(), List.of());

            // 전체 알림 수
            int totalSchedulesCount = calendarList.size() + officialCalendarSize;

            String title = String.format(TITLE_FORMAT, totalSchedulesCount);
            String body = generateMemberScheduleBody(calendarList);
            if (!integratedBody.isBlank()) {
                body += "\n" + integratedBody;
            }

            calendarNotification.saveAndSendForMember(member, devices, title, body);
        });

        // 비회원 디바이스
        String integratedTitle = String.format(TITLE_FORMAT, officialCalendarSize);
        calendarNotification.sendForAnonymous(officialCalendarSize, integratedTitle, integratedBody);
        log.info("Calendar Schedule ended.");
    }

    /**
     * { 회원, 일정 } DTO 리스트를 { 회원: List<일정> } 맵으로 변환
     *
     * @param memberCalendarList { 회원, 일정 } DTO 리스트
     * @return { 회원: List<일정> } 맵
     */
    private Map<Member, List<TodaySchedule>> memberCalendarsToMap(List<TodaySchedule> memberCalendarList) {
        return memberCalendarList.stream()
                .collect(Collectors.groupingBy(TodaySchedule::member));
    }

    /**
     * 일정 제목을 통해 알림 본문 생성
     *
     * @param todaySchedules 일정 목록
     * @return 알림 본문
     */
    private String generateMemberScheduleBody(List<TodaySchedule> todaySchedules) {
        if (todaySchedules.isEmpty()) {
            return "";
        }

        return String.format(MEMBER_SCHEDULE_BODY_FORMAT, todaySchedules.get(0).title(), todaySchedules.size());
    }

    /**
     * 일정 제목을 통해 알림 본문 생성
     *
     * @param todaySchedules 일정 목록
     * @return 알림 본문
     */
    private String generateOfficialScheduleBody(List<String> todaySchedules) {
        if (todaySchedules.isEmpty()) {
            return "";
        }

        return String.format(OFFICIAL_SCHEDULE_BODY_FORMAT, todaySchedules.get(0), todaySchedules.size());
    }
}
