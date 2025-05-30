package com.dongsoop.dongsoop.calendar.repository;

import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import com.dongsoop.dongsoop.calendar.entity.QOfficialSchedule;
import com.querydsl.core.types.dsl.BooleanExpression;
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

    public List<OfficialSchedule> findOfficialScheduleByDuration(LocalDate startAt, LocalDate endAt) {
        return queryFactory.selectFrom(officialSchedule)
                .where(isDurationWithin(startAt, endAt))
                .fetch();
    }

    public BooleanExpression isDurationWithin(LocalDate startAt, LocalDate endAt) {
        BooleanExpression startsWithinDuration = officialSchedule.startAt.goe(startAt)
                .and(officialSchedule.startAt.lt(endAt));

        BooleanExpression endsWithinDuration = officialSchedule.endAt.goe(startAt)
                .and(officialSchedule.endAt.lt(endAt));

        BooleanExpression overlapAllDuration = officialSchedule.startAt.lt(startAt)
                .and(officialSchedule.endAt.gt(endAt));

        return startsWithinDuration
                .or(endsWithinDuration)
                .or(overlapAllDuration);
    }
}
