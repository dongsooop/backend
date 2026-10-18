package com.dongsoop.dongsoop.calendar.notification.query;

import com.dongsoop.dongsoop.calendar.dto.TodaySchedule;
import com.dongsoop.dongsoop.calendar.entity.QMemberSchedule;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaCalendarNotificationQuery implements CalendarNotificationQuery {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final QMemberSchedule memberSchedule = QMemberSchedule.memberSchedule;
    private static final QMember member = QMember.member;
    private static final QMemberDevice memberDevice = QMemberDevice.memberDevice;
    private final JPAQueryFactory queryFactory;

    @Override
    public List<TodaySchedule> findTodaySchedules() {
        LocalDate now = LocalDate.now(KST);

        // startAt to LocalDate
        DateExpression<LocalDate> startAtDate = Expressions.dateTemplate(LocalDate.class, "DATE({0})",
                memberSchedule.startAt);

        return queryFactory.selectDistinct(Projections.constructor(
                        TodaySchedule.class,
                        memberSchedule.id,
                        memberSchedule.title,
                        member))
                .from(member)
                .leftJoin(memberSchedule)
                .on(member.eq(memberSchedule.member).and(startAtDate.eq(now)))
                .innerJoin(memberDevice)
                .on(member.eq(memberDevice.member))
                .fetch();
    }

}
