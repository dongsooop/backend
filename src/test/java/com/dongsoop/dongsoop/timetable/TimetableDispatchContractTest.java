package com.dongsoop.dongsoop.timetable;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.service.MemberDeviceService;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.timetable.dto.TodayTimetable;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.notification.TimetableNotification;
import com.dongsoop.dongsoop.timetable.notification.query.TimetableNotificationQuery;
import com.dongsoop.dongsoop.timetable.schedule.TimetableScheduler;
import java.time.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimetableDispatchContractTest {
    @Mock TimetableNotificationQuery timetables;
    @Mock TimetableNotification notification;
    @Mock MemberDeviceService devices;
    @InjectMocks TimetableScheduler scheduler;

    @Test
    void groupsClassesAndFormatsExactPayloadOnlyForMembersWithDevices() {
        when(timetables.findTodayTimetables(any(), any(), any())).thenReturn(List.of(
                row("알고리즘", 9, 0, 7L), row("데이터베이스", 13, 30, 7L), row("수학", 10, 0, 8L)));
        when(devices.getDeviceByMember(any())).thenReturn(Map.of(7L, List.of("a", "b")));
        LocalDate date = LocalDate.now(ZoneId.of("Asia/Seoul"));
        scheduler.scheduled();
        verify(timetables).findTodayTimetables(Year.of(date.getYear()), SemesterType.fromMonth(date.getMonthValue()), date.getDayOfWeek());
        ArgumentCaptor<MemberDeviceFindCondition> condition = ArgumentCaptor.forClass(MemberDeviceFindCondition.class);
        verify(devices).getDeviceByMember(condition.capture());
        assertThat(condition.getValue().memberIds()).containsOnly(7L, 8L);
        assertThat(condition.getValue().notificationType()).isEqualTo(NotificationType.TIMETABLE);
        verify(notification).send("[시간표 알림] 오늘 2개의 수업이 있습니다", "- 09:00 알고리즘\n- 13:30 데이터베이스", List.of("a", "b"));
        verifyNoMoreInteractions(notification);
    }

    @Test
    void noClassesOrNoSubscribedDevicesNeverSends() {
        scheduler.scheduled();
        when(timetables.findTodayTimetables(any(), any(), any())).thenReturn(List.of(row("수학", 9, 0, 7L)));
        scheduler.scheduled();
        verifyNoInteractions(notification);
    }

    @Test
    void failedTimetableLookupCannotDispatchPartialNotification() {
        when(timetables.findTodayTimetables(any(), any(), any()))
                .thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(scheduler::scheduled).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(devices, notification);
    }

    private TodayTimetable row(String name, int hour, int minute, Long memberId) {
        return new TodayTimetable(name, LocalTime.of(hour, minute), LocalTime.of(hour + 1, minute), memberId);
    }
}
