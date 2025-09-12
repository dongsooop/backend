package com.dongsoop.dongsoop.calendar.repository;

import com.dongsoop.dongsoop.calendar.dto.HomeSchedule;
import com.dongsoop.dongsoop.calendar.dto.ScheduleType;
import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import com.dongsoop.dongsoop.calendar.entity.QOfficialSchedule;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OfficialScheduleRepositoryCustomImpl implements OfficialScheduleRepositoryCustom {


    private static final QOfficialSchedule officialSchedule = QOfficialSchedule.officialSchedule;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<OfficialSchedule> findOfficialScheduleByDuration(LocalDate startAt, LocalDate endAt) {
        return queryFactory.selectFrom(officialSchedule)
                .where(ScheduleDurationChecker.isDurationWithin(
                        officialSchedule.startAt,
                        officialSchedule.endAt,
                        startAt,
                        endAt))
                .fetch();
    }

    @Override
    public List<String> searchTodaySchedule() {
        LocalDate now = LocalDate.now();

        // startAt to LocalDate
        DateExpression<LocalDate> startAtDate = Expressions.dateTemplate(LocalDate.class, "DATE({0})",
                officialSchedule.startAt);

        return queryFactory.select(officialSchedule.title)
                .from(officialSchedule)
                .where(startAtDate.eq(now))
                .fetch();
    }

    @Override
    public List<HomeSchedule> searchHomeSchedule(LocalDate date) {
        // startAt to LocalDate
        DateExpression<LocalDate> startAtDate = Expressions.dateTemplate(LocalDate.class, "DATE({0})",
                officialSchedule.startAt);

        return queryFactory.selectDistinct(Projections.constructor(
                        HomeSchedule.class,
                        officialSchedule.title,
                        officialSchedule.startAt,
                        officialSchedule.endAt,
                        Expressions.constant(ScheduleType.OFFICIAL)))
                .from(officialSchedule)
                .where(startAtDate.eq(date))
                .fetch();
    }
}
