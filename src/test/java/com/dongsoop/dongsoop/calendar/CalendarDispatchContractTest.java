package com.dongsoop.dongsoop.calendar;

import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.calendar.dto.TodaySchedule;
import com.dongsoop.dongsoop.calendar.notification.CalendarNotification;
import com.dongsoop.dongsoop.calendar.notification.query.CalendarNotificationQuery;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.calendar.schedule.CalendarScheduler;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarDispatchContractTest {
    @Mock CalendarNotificationQuery query;
    @Mock OfficialScheduleRepository official;
    @Mock MemberDeviceService devices;
    @Mock CalendarNotification notification;
    @InjectMocks CalendarScheduler scheduler;

    @Test
    void memberWithoutPersonalScheduleStillReceivesOfficialSchedule() {
        var member = Member.builder().id(7L).build();
        when(query.findTodaySchedules()).thenReturn(List.of(new TodaySchedule(null, null, member)));
        when(official.searchTodaySchedule()).thenReturn(List.of("School event"));
        when(devices.getDeviceByMember(any())).thenReturn(Map.of(7L, List.of("token")));

        scheduler.send();

        verify(notification).saveAndSendForMember(member, List.of("token"),
                "[일정 알림] 오늘 1개의 일정이 있습니다", "- 학사 일정: School event");
        verify(notification).sendForAnonymous(1, "[일정 알림] 오늘 1개의 일정이 있습니다", "- 학사 일정: School event");
    }

    @Test
    void missingEligibleDevicesCannotDispatchMemberNotification() {
        var member = Member.builder().id(7L).build();
        when(query.findTodaySchedules()).thenReturn(List.of(new TodaySchedule(19L, "Meeting", member)));
        when(official.searchTodaySchedule()).thenReturn(List.of());
        when(devices.getDeviceByMember(any())).thenReturn(Map.of());
        scheduler.send();
        verify(notification, never()).saveAndSendForMember(any(), anyList(), anyString(), anyString());
    }
}
