package com.dongsoop.dongsoop.timetable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.timetable.dto.CreateTimetableRequest;
import com.dongsoop.dongsoop.timetable.dto.OverlapTimetable;
import com.dongsoop.dongsoop.timetable.dto.TimetableView;
import com.dongsoop.dongsoop.timetable.dto.UpdateTimetableRequest;
import com.dongsoop.dongsoop.timetable.dto.YearSemester;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.dongsoop.dongsoop.timetable.entity.Timetable;
import com.dongsoop.dongsoop.timetable.exception.TimetableNotFoundException;
import com.dongsoop.dongsoop.timetable.exception.TimetableNotOwnedException;
import com.dongsoop.dongsoop.timetable.exception.TimetableOverlapException;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepository;
import com.dongsoop.dongsoop.timetable.service.TimetableMapper;
import com.dongsoop.dongsoop.timetable.service.TimetableServiceImpl;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimetableServiceImplTest {
  @Mock private TimetableRepository repository;
  @Mock private MemberService members;
  @Mock private YearSemester yearSemester;
  private TimetableServiceImpl service;
  private static final Year YEAR = Year.of(2026);
  private static final LocalTime START = LocalTime.of(9, 0);
  private static final LocalTime END = LocalTime.of(10, 0);

  @BeforeEach
  void setUp() {
    service = new TimetableServiceImpl(repository, members, new TimetableMapper(members));
  }

  @Test
  @DisplayName("시간이 겹치지 않는 시간표는 요청 회원과 수업 정보를 저장한다")
  void createsTimetable() {
    Member owner = Member.builder().id(7L).build();
    when(members.getMemberReferenceByContext()).thenReturn(owner);
    when(members.getMemberIdByAuthentication()).thenReturn(7L);

    service.createTimetable(create("수업", START, END));

    ArgumentCaptor<Timetable> captor = ArgumentCaptor.forClass(Timetable.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue())
        .extracting(
            "name",
            "professor",
            "location",
            "week",
            "startAt",
            "endAt",
            "year",
            "semester",
            "member")
        .containsExactly(
            "수업", "교수", "강의실", DayOfWeek.MONDAY, START, END, YEAR, SemesterType.FIRST, owner);
    verify(repository)
        .findOverlapWithinRange(7L, YEAR, SemesterType.FIRST, DayOfWeek.MONDAY, START, END);
  }

  @Test
  @DisplayName("겹치는 시간표가 있으면 저장하지 않는다")
  void rejectsOverlappingCreation() {
    when(members.getMemberIdByAuthentication()).thenReturn(7L);
    when(repository.findOverlapWithinRange(
            7L, YEAR, SemesterType.FIRST, DayOfWeek.MONDAY, START, END))
        .thenReturn(Optional.of(new OverlapTimetable(START, END)));

    CreateTimetableRequest request = create("수업", START, END);
    assertThrows(TimetableOverlapException.class, () -> service.createTimetable(request));

    verify(repository, never()).save(any());
  }

  @Test
  @DisplayName("일괄 생성은 겹친 요청만 실패 목록으로 돌려주고 나머지만 저장한다")
  void bulkCreationSeparatesFailedRequests() {
    CreateTimetableRequest blocked = create("겹침", START, END);
    CreateTimetableRequest allowed = create("가능", LocalTime.NOON, LocalTime.of(13, 0));
    when(members.getMemberIdByAuthentication()).thenReturn(7L);
    when(repository.findOverlapWithinRange(
            7L, YEAR, SemesterType.FIRST, DayOfWeek.MONDAY, START, END))
        .thenReturn(Optional.of(new OverlapTimetable(START, END)));

    assertEquals(List.of(blocked), service.createTimetable(List.of(blocked, allowed)));

    ArgumentCaptor<List<Timetable>> captor = ArgumentCaptor.captor();
    verify(repository).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(1);
    assertThat(captor.getValue().get(0))
        .extracting("name", "startAt", "endAt")
        .containsExactly("가능", LocalTime.NOON, LocalTime.of(13, 0));
  }

  @Test
  @DisplayName("조회는 현재 회원과 요청 연도·학기를 사용한다")
  void listsCurrentMemberTimetable() {
    Member owner = Member.builder().id(7L).build();
    List<TimetableView> expected =
        List.of(new TimetableView(3L, "수업", "교수", "강의실", START, END, DayOfWeek.MONDAY));
    when(members.getMemberReferenceByContext()).thenReturn(owner);
    when(repository.findAllByMemberAndYearAndSemester(owner, YEAR, SemesterType.FIRST))
        .thenReturn(expected);
    assertSame(expected, service.getTimetableView(YEAR, SemesterType.FIRST));
  }

  @Test
  @DisplayName("소유자는 시간표 한 건 또는 해당 학기 전체를 삭제할 수 있다")
  void deletesOwnedTimetableAndSemester() {
    when(members.getMemberIdByAuthentication()).thenReturn(7L);
    when(repository.existsByIdAndMemberId(3L, 7L)).thenReturn(true);

    service.deleteTimetable(3L);
    service.deleteTimetable(YEAR, SemesterType.FIRST);

    verify(repository).deleteById(3L);
    verify(repository).deleteByMemberIdAndYearAndSemester(7L, YEAR, SemesterType.FIRST);
  }

  @Test
  @DisplayName("다른 회원의 시간표는 수정하거나 삭제할 수 없다")
  void nonOwnerCannotMutateTimetable() {
    when(members.getMemberIdByAuthentication()).thenReturn(7L);

    UpdateTimetableRequest request = update();
    assertThrows(TimetableNotOwnedException.class, () -> service.deleteTimetable(3L));
    assertThrows(TimetableNotOwnedException.class, () -> service.updateTimetable(request));

    verify(repository, never()).deleteById(any());
    verify(repository, never()).findYearSemesterById(any());
    verify(repository, never()).findById(any());
  }

  @Test
  @DisplayName("시간표 수정은 자기 자신을 제외한 겹침 검증 후 모든 지정 값을 반영한다")
  void updatesOwnedTimetable() {
    Timetable timetable =
        Timetable.builder().id(3L).name("이전").year(YEAR).semester(SemesterType.FIRST).build();
    stubOwnedYearSemester();
    when(repository.findById(3L)).thenReturn(Optional.of(timetable));

    service.updateTimetable(update());

    verify(repository)
        .findOverlapWithinRangeExcludingSelf(
            3L, 7L, YEAR, SemesterType.FIRST, DayOfWeek.TUESDAY, START, END);
    assertThat(timetable)
        .extracting("name", "professor", "location", "week", "startAt", "endAt", "year", "semester")
        .containsExactly(
            "변경", "교수2", "도서관", DayOfWeek.TUESDAY, START, END, YEAR, SemesterType.FIRST);
  }

  @Test
  @DisplayName("부분 수정은 지정하지 않은 수업 정보를 유지한다")
  void partialUpdatePreservesOmittedFields() {
    Timetable timetable =
        Timetable.builder()
            .id(3L)
            .name("이전")
            .professor("교수")
            .location("강의실")
            .week(DayOfWeek.MONDAY)
            .startAt(START)
            .endAt(END)
            .build();
    stubOwnedYearSemester();
    when(repository.findById(3L)).thenReturn(Optional.of(timetable));

    service.updateTimetable(new UpdateTimetableRequest(3L, "이름만 변경", null, null, null, null, null));

    assertThat(timetable)
        .extracting("name", "professor", "location", "week", "startAt", "endAt")
        .containsExactly("이름만 변경", "교수", "강의실", DayOfWeek.MONDAY, START, END);
  }

  @Test
  @DisplayName("다른 시간표와 겹치는 수정은 원래 값을 유지한다")
  void rejectsOverlappingUpdate() {
    stubOwnedYearSemester();
    when(repository.findOverlapWithinRangeExcludingSelf(
            3L, 7L, YEAR, SemesterType.FIRST, DayOfWeek.TUESDAY, START, END))
        .thenReturn(Optional.of(new OverlapTimetable(START, END)));

    UpdateTimetableRequest request = update();
    assertThrows(TimetableOverlapException.class, () -> service.updateTimetable(request));

    verify(repository, never()).findById(any());
  }

  @Test
  @DisplayName("연도·학기 정보가 없는 수정 대상은 찾을 수 없음 오류를 반환한다")
  void missingSemesterCannotBeUpdated() {
    when(members.getMemberIdByAuthentication()).thenReturn(7L);
    when(repository.existsByIdAndMemberId(3L, 7L)).thenReturn(true);
    UpdateTimetableRequest request = update();
    assertThrows(TimetableNotFoundException.class, () -> service.updateTimetable(request));
    verify(repository, never()).findById(any());
  }

  @Test
  @DisplayName("소유권 확인 이후 대상이 없어져도 찾을 수 없음 오류를 반환한다")
  void missingTimetableCannotBeUpdated() {
    stubOwnedYearSemester();
    UpdateTimetableRequest request = update();
    assertThrows(TimetableNotFoundException.class, () -> service.updateTimetable(request));
  }

  private void stubOwnedYearSemester() {
    when(members.getMemberIdByAuthentication()).thenReturn(7L);
    when(repository.existsByIdAndMemberId(3L, 7L)).thenReturn(true);
    when(repository.findYearSemesterById(3L)).thenReturn(Optional.of(yearSemester));
    when(yearSemester.getYear()).thenReturn(YEAR);
    when(yearSemester.getSemester()).thenReturn(SemesterType.FIRST);
  }

  private CreateTimetableRequest create(String name, LocalTime start, LocalTime end) {
    return new CreateTimetableRequest(
        name, "교수", "강의실", DayOfWeek.MONDAY, start, end, YEAR, SemesterType.FIRST);
  }

  private UpdateTimetableRequest update() {
    return new UpdateTimetableRequest(3L, "변경", "교수2", "도서관", DayOfWeek.TUESDAY, START, END);
  }
}
