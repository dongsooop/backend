package com.dongsoop.dongsoop.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.calendar.dto.CreateMemberScheduleRequest;
import com.dongsoop.dongsoop.calendar.dto.MemberScheduleUpdateRequest;
import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.dto.ScheduleType;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import com.dongsoop.dongsoop.calendar.exception.MemberScheduleNotFoundException;
import com.dongsoop.dongsoop.calendar.exception.NotScheduleOwnerException;
import com.dongsoop.dongsoop.calendar.exception.ScheduleAlreadySetByMemberException;
import com.dongsoop.dongsoop.calendar.repository.MemberScheduleRepository;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.dongsoop.dongsoop.calendar.service.ScheduleServiceImpl;
import com.dongsoop.dongsoop.member.entity.Member;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {
    @Mock private MemberScheduleRepository personal;
    @Mock private OfficialScheduleRepository official;
    private ScheduleServiceImpl service;
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 13, 9, 0);

    @BeforeEach
    void setUp() {
        service = new ScheduleServiceImpl(personal, official);
    }

    @ParameterizedTest
    @CsvSource({"2024-02,2024-01-28,2024-03-03", "2026-03,2026-02-22,2026-04-05",
            "2027-01,2026-12-27,2027-02-07"})
    @DisplayName("달력 조회는 월 경계의 일요일부터 다음 일요일까지 확장하고 윤년과 연도 변경을 처리한다")
    void queriesCalendarWindow(String month, String start, String end) {
        service.getSchedule(7L, YearMonth.parse(month));

        verify(official).findOfficialScheduleByDuration(LocalDate.parse(start), LocalDate.parse(end));
        verify(personal).findMemberScheduleByDuration(7L, LocalDate.parse(start).atStartOfDay(),
                LocalDate.parse(end).atStartOfDay());
    }

    @Test
    @DisplayName("회원 일정은 공식 일정과 개인 일정을 시작 시각과 종료 시각 순서로 조합한다")
    void combinesAndSortsMemberSchedules() {
        MemberSchedule later = schedule(1L, "늦은 일정", START.plusHours(2));
        MemberSchedule longer = schedule(2L, "긴 일정", START);
        MemberSchedule shorter = MemberSchedule.builder().id(3L).title("짧은 일정")
                .startAt(START).endAt(START.plusMinutes(30)).build();
        OfficialSchedule allDay = OfficialSchedule.builder().title("공식")
                .startAt(START.toLocalDate()).endAt(START.toLocalDate()).build();
        when(official.findOfficialScheduleByDuration(any(), any())).thenReturn(List.of(allDay));
        when(personal.findMemberScheduleByDuration(any(), any(), any())).thenReturn(List.of(later, longer, shorter));

        List<ScheduleDetails> result = service.getSchedule(7L, YearMonth.of(2026, 10));

        assertEquals(List.of("공식", "짧은 일정", "긴 일정", "늦은 일정"),
                result.stream().map(ScheduleDetails::getTitle).toList());
        assertEquals(ScheduleType.OFFICIAL, result.get(0).getType());
        assertEquals(START.toLocalDate().atTime(23, 59, 59), result.get(0).getEndAt());
        assertEquals(ScheduleType.MEMBER, result.get(1).getType());
        assertEquals(3L, result.get(1).getId());
    }

    @Test
    @DisplayName("비회원은 정렬된 공식 일정만 받으며 개인 일정 저장소를 조회하지 않는다")
    void guestOnlyReceivesOfficialSchedules() {
        OfficialSchedule later = OfficialSchedule.builder().title("나중")
                .startAt(LocalDate.of(2026, 10, 20)).endAt(LocalDate.of(2026, 10, 21)).build();
        OfficialSchedule earlier = OfficialSchedule.builder().title("먼저")
                .startAt(LocalDate.of(2026, 10, 10)).endAt(LocalDate.of(2026, 10, 11)).build();
        when(official.findOfficialScheduleByDuration(any(), any())).thenReturn(List.of(later, earlier));

        assertEquals(List.of("먼저", "나중"), service.getSchedule(YearMonth.of(2026, 10)).stream()
                .map(ScheduleDetails::getTitle).toList());
        verifyNoInteractions(personal);
    }

    @Test
    @DisplayName("개인 일정 생성은 입력과 요청 회원을 저장한다")
    void createsScheduleForRequester() {
        Member owner = Member.builder().id(7L).build();
        CreateMemberScheduleRequest request = new CreateMemberScheduleRequest("일정", "강의실", START,
                START.plusHours(1), "123ABC");
        when(personal.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MemberSchedule result = service.createMemberSchedule(owner, request);

        assertSame(owner, result.getMember());
        assertEquals("일정", result.toDetails().getTitle());
        assertEquals("강의실", result.toDetails().getLocation());
        assertEquals(START, result.toDetails().getStartAt());
        assertEquals(START.plusHours(1), result.toDetails().getEndAt());
        assertEquals("123ABC", result.toDetails().getColor());
        Member replacement = Member.builder().id(8L).build();
        assertThrows(ScheduleAlreadySetByMemberException.class, () -> result.setMember(replacement));
        assertSame(owner, result.getMember());
    }

    @Test
    @DisplayName("소유자는 일정을 삭제할 수 있다")
    void ownerDeletesSchedule() {
        when(personal.findById(3L)).thenReturn(Optional.of(schedule(3L, "일정", START)));
        service.deleteMemberSchedule(3L, 7L);
        verify(personal).deleteById(3L);
    }

    @Test
    @DisplayName("부분 수정은 지정하지 않은 값을 유지한다")
    void partialUpdatePreservesOmittedFields() {
        MemberSchedule schedule = schedule(3L, "기존", START);
        when(personal.findById(3L)).thenReturn(Optional.of(schedule));

        service.updateMemberSchedule(3L, 7L, new MemberScheduleUpdateRequest("변경", null, null, null, null));

        assertEquals("변경", schedule.toDetails().getTitle());
        assertEquals("강의실", schedule.toDetails().getLocation());
        assertEquals(START, schedule.toDetails().getStartAt());
        assertEquals(START.plusHours(1), schedule.toDetails().getEndAt());
        assertEquals("123ABC", schedule.toDetails().getColor());
        verify(personal).save(schedule);
    }

    @Test
    @DisplayName("전체 수정은 모든 입력 필드를 반영한다")
    void fullUpdateChangesAllFields() {
        MemberSchedule schedule = schedule(3L, "기존", START);
        when(personal.findById(3L)).thenReturn(Optional.of(schedule));
        MemberScheduleUpdateRequest request = new MemberScheduleUpdateRequest("변경", "도서관",
                START.plusDays(1), START.plusDays(1).plusHours(2), "FFFFFF");

        service.updateMemberSchedule(3L, 7L, request);

        ScheduleDetails result = schedule.toDetails();
        assertEquals(request.getTitle(), result.getTitle());
        assertEquals(request.getLocation(), result.getLocation());
        assertEquals(request.getStartAt(), result.getStartAt());
        assertEquals(request.getEndAt(), result.getEndAt());
        assertEquals(request.getColor(), result.getColor());
    }

    @Test
    @DisplayName("다른 회원은 일정을 수정하거나 삭제할 수 없다")
    void nonOwnerCannotMutateSchedule() {
        MemberSchedule schedule = schedule(3L, "기존", START);
        when(personal.findById(3L)).thenReturn(Optional.of(schedule));

        MemberScheduleUpdateRequest request = new MemberScheduleUpdateRequest("변경", null, null, null, null);
        assertThrows(NotScheduleOwnerException.class, () -> service.deleteMemberSchedule(3L, 8L));
        assertThrows(NotScheduleOwnerException.class, () -> service.updateMemberSchedule(3L, 8L, request));

        assertEquals("기존", schedule.toDetails().getTitle());
        verify(personal, never()).save(any());
        verify(personal, never()).deleteById(any());
    }

    @Test
    @DisplayName("없는 일정 수정과 삭제는 저장소 변경 없이 실패한다")
    void missingScheduleCannotBeMutated() {
        MemberScheduleUpdateRequest request = new MemberScheduleUpdateRequest("변경", null, null, null, null);
        assertThrows(MemberScheduleNotFoundException.class, () -> service.deleteMemberSchedule(3L, 7L));
        assertThrows(MemberScheduleNotFoundException.class, () -> service.updateMemberSchedule(3L, 7L, request));
        verify(personal, never()).save(any());
        verify(personal, never()).deleteById(any());
    }

    private MemberSchedule schedule(Long id, String title, LocalDateTime start) {
        return MemberSchedule.builder().id(id).title(title).location("강의실")
                .startAt(start).endAt(start.plusHours(1)).color("123ABC")
                .member(Member.builder().id(7L).build()).build();
    }
}
