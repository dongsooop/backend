package com.dongsoop.dongsoop.timetable;

import static org.assertj.core.api.Assertions.*;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.entity.*;
import com.dongsoop.dongsoop.timetable.dto.*;
import com.dongsoop.dongsoop.timetable.entity.*;
import com.dongsoop.dongsoop.timetable.repository.TimetableRepositoryCustomImpl;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Year;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TimetablePersistenceContractTest.QueryConfig.class)
class TimetablePersistenceContractTest extends AbstractIntegrationTest {
  private static final Year YEAR = Year.of(2026);
  @Autowired EntityManager em;
  @Autowired TimetableRepositoryCustomImpl queries;
  Member first;
  Member second;

  @TestConfiguration
  static class QueryConfig {
    @Bean
    JPAQueryFactory queryFactory(EntityManager em) {
      return new JPAQueryFactory(em);
    }
  }

  @BeforeEach
  void fixtures() {
    first = member("first");
    second = member("second");
  }

  @ParameterizedTest
  @CsvSource({
    "08:00,09:00,false",
    "11:00,12:00,false",
    "08:59,09:01,true",
    "09:00,11:00,true",
    "10:00,10:30,true",
    "10:59,12:00,true"
  })
  void overlapUsesOpenTimeBoundary(String start, String end, boolean expected) {
    timetable(first, "class", 9, 11, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    Long memberId = first.getId();
    flushAndClear();
    var overlap =
        queries.findOverlapWithinRange(
            memberId,
            YEAR,
            SemesterType.SECOND,
            DayOfWeek.MONDAY,
            LocalTime.parse(start),
            LocalTime.parse(end));
    assertThat(overlap.isPresent()).isEqualTo(expected);
    overlap.ifPresent(
        value -> {
          assertThat(value.startAt()).isEqualTo(LocalTime.of(9, 0));
          assertThat(value.endAt()).isEqualTo(LocalTime.of(11, 0));
        });
  }

  @Test
  void overlapScopesOwnerYearSemesterAndWeekAndCanExcludeSelf() {
    Long own = timetable(first, "own", 9, 11, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    timetable(second, "other", 9, 11, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    timetable(first, "year", 9, 11, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR.minusYears(1));
    timetable(first, "semester", 9, 11, SemesterType.FIRST, DayOfWeek.MONDAY, YEAR);
    timetable(first, "week", 9, 11, SemesterType.SECOND, DayOfWeek.TUESDAY, YEAR);
    Long id = first.getId();
    flushAndClear();
    assertThat(
            queries.findOverlapWithinRangeExcludingSelf(
                own,
                id,
                YEAR,
                SemesterType.SECOND,
                DayOfWeek.MONDAY,
                LocalTime.of(9, 30),
                LocalTime.of(10, 0)))
        .isEmpty();
    assertThat(queries.existsByIdAndMemberId(own, id)).isTrue();
    assertThat(queries.existsByIdAndMemberId(own, second.getId())).isFalse();
    assertThat(queries.existsByIdAndMemberId(-1L, id)).isFalse();
    timetable(
        em.getReference(Member.class, id),
        "conflict",
        10,
        12,
        SemesterType.SECOND,
        DayOfWeek.MONDAY,
        YEAR);
    flushAndClear();
    assertThat(
            queries.findOverlapWithinRangeExcludingSelf(
                own,
                id,
                YEAR,
                SemesterType.SECOND,
                DayOfWeek.MONDAY,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0)))
        .isPresent();
  }

  @Test
  void semesterDeletionOnlySoftDeletesRequestedMembersTerm() {
    Long deleted = timetable(first, "delete", 9, 10, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    Long otherSemester =
        timetable(first, "keep", 9, 10, SemesterType.FIRST, DayOfWeek.MONDAY, YEAR);
    Long otherMember =
        timetable(second, "other", 9, 10, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    Long id = first.getId();
    flushAndClear();
    queries.deleteByMemberIdAndYearAndSemester(id, YEAR, SemesterType.SECOND);
    flushAndClear();
    assertThat(em.find(Timetable.class, deleted)).isNull();
    assertThat(em.find(Timetable.class, otherSemester)).isNotNull();
    assertThat(em.find(Timetable.class, otherMember)).isNotNull();
    assertThat(
            em.createNativeQuery("select is_deleted from timetable where id = :id")
                .setParameter("id", deleted)
                .getSingleResult())
        .isEqualTo(true);
    assertThat(queries.searchHomeTimetable(id, YEAR, SemesterType.SECOND, DayOfWeek.MONDAY))
        .isEmpty();
  }

  @Test
  void notificationProjectionDeduplicatesDeviceJoinAndFiltersMissingDevicesAndDeletedTerm() {
    timetable(first, "late", 11, 12, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    timetable(first, "early", 9, 10, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    timetable(second, "no-device", 8, 9, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    timetable(first, "other-day", 8, 9, SemesterType.SECOND, DayOfWeek.TUESDAY, YEAR);
    em.persist(
        MemberDevice.builder()
            .member(first)
            .deviceToken("a")
            .memberDeviceType(MemberDeviceType.IOS)
            .build());
    em.persist(
        MemberDevice.builder()
            .member(first)
            .deviceToken("b")
            .memberDeviceType(MemberDeviceType.ANDROID)
            .build());
    Long id = first.getId();
    flushAndClear();
    var result =
        queries.getTimetableNotificationDtoList(YEAR, SemesterType.SECOND, DayOfWeek.MONDAY);
    assertThat(result).extracting(TodayTimetable::name).containsExactly("early", "late");
    assertThat(result).extracting(TodayTimetable::memberId).containsOnly(id);
    queries.deleteByMemberIdAndYearAndSemester(id, YEAR, SemesterType.SECOND);
    flushAndClear();
    assertThat(queries.getTimetableNotificationDtoList(YEAR, SemesterType.SECOND, DayOfWeek.MONDAY))
        .isEmpty();
  }

  @Test
  void homeReturnsEarliestThreeClassesOnlyForRequestedScope() {
    timetable(first, "fourth", 14, 15, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    timetable(first, "second", 10, 11, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    timetable(first, "first", 9, 10, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    timetable(first, "third", 12, 13, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    timetable(second, "other-member", 8, 9, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR);
    timetable(first, "other-year", 8, 9, SemesterType.SECOND, DayOfWeek.MONDAY, YEAR.minusYears(1));
    Long id = first.getId();
    flushAndClear();
    assertThat(queries.searchHomeTimetable(id, YEAR, SemesterType.SECOND, DayOfWeek.MONDAY))
        .extracting(HomeTimetable::title)
        .containsExactly("first", "second", "third");
    assertThat(queries.searchHomeTimetable(id, YEAR, SemesterType.SUMMER, DayOfWeek.MONDAY))
        .isEmpty();
  }

  private Long timetable(
      Member owner,
      String title,
      int start,
      int end,
      SemesterType semester,
      DayOfWeek week,
      Year year) {
    Timetable value =
        Timetable.builder()
            .member(owner)
            .name(title)
            .startAt(LocalTime.of(start, 0))
            .endAt(LocalTime.of(end, 0))
            .semester(semester)
            .week(week)
            .year(year)
            .build();
    em.persist(value);
    return (Long) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(value);
  }

  private Member member(String name) {
    Member value =
        Member.builder().email(name + "@example.com").nickname(name).password("encoded").build();
    em.persist(value);
    return value;
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }
}
