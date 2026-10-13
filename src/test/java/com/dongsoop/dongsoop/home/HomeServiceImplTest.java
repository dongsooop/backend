package com.dongsoop.dongsoop.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.calendar.dto.HomeSchedule;
import com.dongsoop.dongsoop.calendar.dto.ScheduleType;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.eclass.service.EclassAssignmentService;
import com.dongsoop.dongsoop.home.dto.HomeDto;
import com.dongsoop.dongsoop.home.dto.HomeEclassSummary;
import com.dongsoop.dongsoop.home.exception.HomeAsyncException;
import com.dongsoop.dongsoop.home.service.HomeServiceImpl;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.recruitment.repository.RecruitmentRepository;
import com.dongsoop.dongsoop.timetable.dto.HomeTimetable;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepository;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HomeServiceImplTest {

    @Mock private ExecutorService executor;
    @Mock private TimetableRepository timetables;
    @Mock private OfficialScheduleRepository officialSchedules;
    @Mock private MemberScheduleRepository memberSchedules;
    @Mock private NoticeRepository notices;
    @Mock private RecruitmentRepository recruitments;
    @Mock private EclassAssignmentService assignments;
    private HomeServiceImpl service;

    @BeforeEach
    void setUp() {
        // 스레드 스케줄링에 의존하지 않고 Future의 데이터 조합과 오류 계약을 검증한다.
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any(Runnable.class));
        service = new HomeServiceImpl(executor, timetables, officialSchedules, memberSchedules,
                notices, recruitments, assignments);
        ReflectionTestUtils.setField(service, "timeoutSeconds", 3);
    }

    @Test
    @DisplayName("회원 홈은 시간표와 과제 요약을 유지하고 개인·공식 일정을 시작 및 종료 시각으로 정렬한다")
    void memberHomeCombinesAndSortsSchedules() {
        HomeSchedule later = schedule("공식", 11, 12, ScheduleType.OFFICIAL);
        HomeSchedule longer = schedule("긴 개인 일정", 9, 11, ScheduleType.MEMBER);
        HomeSchedule shorter = schedule("짧은 개인 일정", 9, 10, ScheduleType.MEMBER);
        List<HomeTimetable> timetable = List.of(new HomeTimetable("수업", LocalTime.NOON, LocalTime.of(13, 0)));
        HomeEclassSummary summary = HomeEclassSummary.unlinked();
        when(timetables.searchHomeTimetable(eq(7L), any(), any(), any())).thenReturn(timetable);
        when(officialSchedules.searchHomeSchedule(any())).thenReturn(List.of(later));
        when(memberSchedules.searchHomeSchedule(eq(7L), any())).thenReturn(List.of(longer, shorter));
        when(assignments.getHomeSummary(7L, "fid", "device")).thenReturn(summary);

        HomeDto result = service.getHome(7L, DepartmentType.DEPT_2001, "fid", "device");

        assertEquals(List.of(shorter, longer, later), result.schedules());
        assertEquals(timetable, result.timetable());
        assertSame(summary, result.eclass_assignment());
        verify(notices).searchHomeNotices(DepartmentType.DEPT_2001);
        verify(recruitments).searchHomeRecruitment("DEPT_2001");
    }

    @Test
    @DisplayName("구독 학과가 없으면 모집을 학과 필터 없이 조회하고 시간표는 비워 둔다")
    void noSubscriptionsUsesUnfilteredRecruitment() {
        when(assignments.getHomeSummary("fid", "device")).thenReturn(HomeEclassSummary.expired());
        HomeSchedule later = schedule("늦은 일정", 11, 12, ScheduleType.OFFICIAL);
        HomeSchedule earlier = schedule("이른 일정", 9, 10, ScheduleType.OFFICIAL);
        when(officialSchedules.searchHomeSchedule(any())).thenReturn(List.of(later, earlier));

        HomeDto result = service.getHome(Set.of(), "fid", "device");

        assertEquals(List.of(earlier, later), result.schedules());
        assertEquals(List.of(), result.timetable());
        assertEquals(HomeEclassSummary.expired(), result.eclass_assignment());
        verify(notices).searchHomeNotices(Set.of());
        verify(recruitments).searchHomeRecruitment();
        verify(recruitments, never()).searchHomeRecruitment(any(String.class));
        verify(memberSchedules, never()).searchHomeSchedule(anyLong(), any());
    }

    @Test
    @DisplayName("모집은 첫 구독 학과로 조회하고 공지는 모든 구독 학과로 조회한다")
    void subscriptionsUseFirstDepartmentForRecruitment() {
        Set<DepartmentType> departments = new LinkedHashSet<>(
                List.of(DepartmentType.DEPT_2002, DepartmentType.DEPT_2001));

        service.getHome(departments, "fid", "device");

        verify(recruitments).searchHomeRecruitment("DEPT_2002");
        verify(recruitments, never()).searchHomeRecruitment();
        verify(notices).searchHomeNotices(departments);
    }

    @Test
    @DisplayName("회원 홈의 조회 실패는 부분 응답 대신 HomeAsyncException을 전달한다")
    void memberHomePropagatesFailure() {
        IllegalStateException cause = new IllegalStateException("조회 실패");
        when(timetables.searchHomeTimetable(eq(7L), any(), any(), any())).thenThrow(cause);

        CompletionException exception = assertThrows(CompletionException.class,
                () -> service.getHome(7L, DepartmentType.DEPT_2001, "fid", "device"));

        HomeAsyncException wrapped = assertInstanceOf(HomeAsyncException.class, exception.getCause());
        assertSame(cause, wrapped.getCause().getCause());
    }

    @Test
    @DisplayName("구독 홈의 과제 조회 실패도 HomeAsyncException으로 전달한다")
    void subscriptionHomePropagatesFailure() {
        IllegalStateException cause = new IllegalStateException("과제 조회 실패");
        when(assignments.getHomeSummary("fid", "device")).thenThrow(cause);

        CompletionException exception = assertThrows(CompletionException.class,
                () -> service.getHome(Set.of(), "fid", "device"));

        HomeAsyncException wrapped = assertInstanceOf(HomeAsyncException.class, exception.getCause());
        assertSame(cause, wrapped.getCause().getCause());
    }

    private HomeSchedule schedule(String title, int startHour, int endHour, ScheduleType type) {
        return new HomeSchedule(title, LocalTime.of(startHour, 0), LocalTime.of(endHour, 0), type);
    }
}
