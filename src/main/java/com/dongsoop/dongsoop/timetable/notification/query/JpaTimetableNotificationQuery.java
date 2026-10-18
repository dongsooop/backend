package com.dongsoop.dongsoop.timetable.notification.query;

import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.dongsoop.dongsoop.timetable.dto.TodayTimetable;
import com.dongsoop.dongsoop.timetable.entity.QTimetable;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.DayOfWeek;
import java.time.Year;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaTimetableNotificationQuery implements TimetableNotificationQuery {
    private static final QTimetable timetable = QTimetable.timetable;
    private static final QMember member = QMember.member;
    private static final QMemberDevice memberDevice = QMemberDevice.memberDevice;
    private final JPAQueryFactory queryFactory;

    @Override
    public List<TodayTimetable> findTodayTimetables(Year year, SemesterType semester,
                                                                DayOfWeek week) {
        return queryFactory
                .selectDistinct(Projections.constructor(TodayTimetable.class,
                        timetable.name,
                        timetable.startAt,
                        timetable.endAt,
                        member.id))
                .from(timetable)
                .innerJoin(timetable.member, member)
                .innerJoin(memberDevice)
                .on(member.eq(memberDevice.member))
                .where(timetable.year.eq(year)
                        .and(timetable.semester.eq(semester))
                        .and(timetable.week.eq(week))
                        .and(timetable.isDeleted.isFalse()))
                .orderBy(timetable.startAt.asc(), timetable.endAt.asc())
                .fetch();
    }

}
