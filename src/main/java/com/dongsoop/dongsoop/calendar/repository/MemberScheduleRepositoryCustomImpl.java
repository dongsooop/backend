package com.dongsoop.dongsoop.calendar.repository;

import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import com.dongsoop.dongsoop.calendar.entity.QMemberSchedule;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberScheduleRepositoryCustomImpl implements MemberScheduleRepositoryCustom {

    private static final QMemberSchedule memberSchedule = QMemberSchedule.memberSchedule;
    
    private final JPAQueryFactory queryFactory;

    public List<MemberSchedule> findMemberScheduleByDuration(Long memberId, LocalDateTime startAt,
                                                             LocalDateTime endAt) {
        return queryFactory.selectFrom(memberSchedule)
                .where(memberSchedule.member.id.eq(memberId))
                .where(isDurationWithin(startAt, endAt))
                .fetch();
    }

    public BooleanExpression isDurationWithin(LocalDateTime startAt, LocalDateTime endAt) {
        BooleanExpression a = memberSchedule.startAt.goe(startAt)
                .and(memberSchedule.startAt.lt(endAt));

        BooleanExpression b = memberSchedule.endAt.goe(startAt)
                .and(memberSchedule.endAt.lt(endAt));

        return a.or(b);
    }
}
