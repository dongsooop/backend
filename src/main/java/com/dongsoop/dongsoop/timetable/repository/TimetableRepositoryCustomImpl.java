package com.dongsoop.dongsoop.timetable.repository;

import com.dongsoop.dongsoop.timetable.dto.OverlapTimetable;
import com.dongsoop.dongsoop.timetable.entity.QTimetable;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Year;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TimetableRepositoryCustomImpl implements TimetableRepositoryCustom {

    private static final QTimetable timetable = QTimetable.timetable;

    private final JPAQueryFactory queryFactory;

    public Optional<OverlapTimetable> findOverlapWithinRange(Long memberId, Year year, SemesterType semester,
                                                             DayOfWeek week,
                                                             LocalTime startAt, LocalTime endAt) {
        OverlapTimetable result = queryFactory
                .select(Projections.constructor(OverlapTimetable.class,
                        timetable.startAt, timetable.endAt))
                .from(timetable)
                .where(timetable.semester.eq(semester)
                        .and(timetable.year.eq(year))
                        .and(timetable.member.id.eq(memberId))
                        .and(timetable.week.eq(week))
                        .and(validateOverlap(startAt, endAt)))
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    private BooleanExpression validateOverlap(LocalTime startAt, LocalTime endAt) {
        return timetable.startAt.lt(endAt).and(timetable.endAt.gt(startAt));
    }

    public boolean existsByIdAndMemberId(Long id, Long memberId) {
        return queryFactory
                .selectFrom(timetable)
                .where(timetable.id.eq(id)
                        .and(timetable.member.id.eq(memberId)))
                .fetchOne() != null;
    }

    public Optional<OverlapTimetable> findOverlapWithinRangeExcludingSelf(Long timetableId, Long memberId, Year year,
                                                                          SemesterType semester,
                                                                          DayOfWeek week,
                                                                          LocalTime startAt, LocalTime endAt) {
        OverlapTimetable result = queryFactory
                .select(Projections.constructor(OverlapTimetable.class,
                        timetable.startAt, timetable.endAt))
                .from(timetable)
                .where(timetable.semester.eq(semester)
                        .and(timetable.year.eq(year))
                        .and(timetable.member.id.eq(memberId))
                        .and(timetable.week.eq(week))
                        .and(validateOverlap(startAt, endAt))
                        .and(timetable.id.ne(timetableId)))
                .fetchFirst();

        return Optional.ofNullable(result);
    }

    public void deleteByMemberIdAndYearAndSemester(Long memberId, Year year, SemesterType semester) {
        queryFactory
                .update(timetable)
                .set(timetable.member.isDeleted, true)
                .where(timetable.member.id.eq(memberId)
                        .and(timetable.year.eq(year))
                        .and(timetable.semester.eq(semester)))
                .execute();
    }
}
