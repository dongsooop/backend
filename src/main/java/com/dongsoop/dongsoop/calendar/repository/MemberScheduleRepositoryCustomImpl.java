package com.dongsoop.dongsoop.calendar.repository;

import com.dongsoop.dongsoop.calendar.dto.HomeSchedule;
import com.dongsoop.dongsoop.calendar.dto.ScheduleType;
import com.dongsoop.dongsoop.calendar.dto.TodaySchedule;
import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.entity.QMemberSchedule;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.DateExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberScheduleRepositoryCustomImpl implements MemberScheduleRepositoryCustom {

    private static final QMemberSchedule memberSchedule = QMemberSchedule.memberSchedule;
    private static final QMemberDevice memberDevice = QMemberDevice.memberDevice;
    private static final QMember member = QMember.member;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberSchedule> findMemberScheduleByDuration(Long memberId, LocalDateTime startAt,
                                                             LocalDateTime endAt) {
        return queryFactory.selectFrom(memberSchedule)
                .where(memberSchedule.member.id.eq(memberId))
                .where(ScheduleDurationChecker.isDurationWithin(
                        memberSchedule.startAt,
                        memberSchedule.endAt,
                        startAt,
                        endAt))
                .fetch();
    }

    @Override
    public List<TodaySchedule> searchTodaySchedule() {
        LocalDate now = LocalDate.now();

        // startAt to LocalDate
        DateExpression<LocalDate> startAtDate = Expressions.dateTemplate(LocalDate.class, "DATE({0})",
                memberSchedule.startAt);

        return queryFactory.selectDistinct(Projections.constructor(
                        TodaySchedule.class,
                        memberSchedule.title,
                        member))
                .from(memberSchedule)
                .rightJoin(memberSchedule.member, member)
                .innerJoin(memberDevice)
                .on(member.eq(memberDevice.member))
                .where(startAtDate.eq(now))
                .fetch();
    }

    @Override
    public List<HomeSchedule> searchHomeSchedule(Long memberId, LocalDate date) {
        // startAt to LocalDate
        DateExpression<LocalDate> startAtDate = Expressions.dateTemplate(LocalDate.class, "DATE({0})",
                memberSchedule.startAt);

        return queryFactory.selectDistinct(Projections.constructor(
                        HomeSchedule.class,
                        memberSchedule.title,
                        memberSchedule.startAt,
                        memberSchedule.endAt,
                        Expressions.constant(ScheduleType.MEMBER)))
                .from(memberSchedule)
                .innerJoin(memberSchedule.member, member)
                .where(startAtDate.eq(date)
                        .and(member.id.eq(memberId)))
                .fetch();
    }
}
