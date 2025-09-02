package com.dongsoop.dongsoop.calendar.schedule;

import com.dongsoop.dongsoop.calendar.dto.TodaySchedule;
import com.dongsoop.dongsoop.calendar.notification.CalendarNotification;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CalendarSchedulerImpl implements CalendarScheduler {

    private static final Long NON_SAVE_NOTIFICATION_ID = -1L;
    private static final String TITLE_FORMAT = "[일정 알림] 오늘 %d개의 일정이 있습니다";

    private final MemberDeviceService memberDeviceService;
    private final MemberScheduleRepository memberScheduleRepository;
    private final OfficialScheduleRepository officialScheduleRepository;
    private final NotificationSaveService notificationSaveService;
    private final NotificationSendService notificationSendService;
    private final CalendarNotification calendarNotification;

    @Value("${notification.topic.anonymous}")
    private String anonymousTopic;

    @Override
    @Scheduled(cron = "0 0 8 * * *")
    public void send() {
        // 회원 일정 및 공통 일정 조회
        List<TodaySchedule> memberCalendarList = memberScheduleRepository.searchTodaySchedule();
        List<String> officialCalendarList = officialScheduleRepository.searchTodaySchedule();

        Map<Member, List<String>> membercalendarMap = memberCalendarsToMap(memberCalendarList);
        List<Long> targetIdList = memberCalendarList.stream()
                .map(calendarNotification -> calendarNotification.member().getId())
                .toList();

        Map<Long, List<String>> targetDevices = memberDeviceService.getDeviceByMember(targetIdList);

        // 공통 일정 수 및 내용
        int officialCalendarSize = officialCalendarList.size();
        String integratedBody = generateBody(officialCalendarList);

        // 회원 디바이스
        membercalendarMap.forEach((member, calendarList) -> {
            if (member == null) {
                return;
            }

            // 타겟 회원의 디바이스 목록
            List<String> devices = targetDevices.getOrDefault(member.getId(), List.of());

            // 전체 알림 수
            int totalSchedulesCount = calendarList.size() + officialCalendarSize;

            String title = String.format(TITLE_FORMAT, totalSchedulesCount);
            String body = generateBody(calendarList);
            if (!integratedBody.isBlank()) {
                body += "\n" + integratedBody;
            }

            calendarNotification.saveAndSendForMember(member, devices, title, body);
        });

        // 비회원 디바이스
        calendarNotification.sendForAnonymous(officialCalendarSize, integratedBody);
    }

    /**
     * { 회원, 일정 } DTO 리스트를 { 회원: List<일정> } 맵으로 변환
     *
     * @param memberCalendarList { 회원, 일정 } DTO 리스트
     * @return { 회원: List<일정> } 맵
     */
    private Map<Member, List<String>> memberCalendarsToMap(List<TodaySchedule> memberCalendarList) {
        return memberCalendarList.stream()
                .collect(Collectors.groupingBy(TodaySchedule::member,
                        Collectors.mapping(
                                TodaySchedule::title,
                                Collectors.toList()))
                );
    }

    /**
     * 일정 제목을 통해 알림 본문 생성
     *
     * @param titles 일정 제목 목록
     * @return 알림 본문
     */
    private String generateBody(List<String> titles) {
        StringBuilder bodyBuilder = new StringBuilder();

        for (String title : titles) {
            bodyBuilder
                    .append("- ")
                    .append(title)
                    .append("\n");
        }

        return bodyBuilder.toString().trim();
    }
}
