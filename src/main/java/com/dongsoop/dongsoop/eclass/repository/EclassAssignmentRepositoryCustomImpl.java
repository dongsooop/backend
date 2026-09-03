package com.dongsoop.dongsoop.eclass.repository;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import com.dongsoop.dongsoop.eclass.entity.QEclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.QEclassLink;
import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EclassAssignmentRepositoryCustomImpl implements EclassAssignmentRepositoryCustom {

    private static final QEclassAssignment assignment = QEclassAssignment.eclassAssignment;
    private static final QEclassLink link = QEclassLink.eclassLink;
    private static final QMemberDevice device = QMemberDevice.memberDevice;

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EclassAssignment> searchReminderTargets(LocalDateTime from, LocalDateTime to) {
        return queryFactory.selectFrom(assignment)
                .join(assignment.link, link).fetchJoin()
                .join(link.device, device).fetchJoin()
                .leftJoin(device.member).fetchJoin()
                .where(link.status.eq(EclassLinkStatus.ACTIVE),
                        isPending(),
                        assignment.dueAt.gt(from),
                        assignment.dueAt.loe(to))
                .orderBy(assignment.dueAt.asc())
                .fetch();
    }

    @Override
    public List<EclassAssignment> searchUpcomingByDevice(Long deviceId, LocalDateTime now, int limit) {
        return search(device.id.eq(deviceId), now, limit);
    }

    @Override
    public List<EclassAssignment> searchUpcomingByMember(Long memberId, LocalDateTime now, int limit) {
        return search(device.member.id.eq(memberId), now, limit);
    }

    @Override
    public long countUpcomingByDevice(Long deviceId, LocalDateTime now) {
        return count(device.id.eq(deviceId), now);
    }

    @Override
    public long countUpcomingByMember(Long memberId, LocalDateTime now) {
        return count(device.member.id.eq(memberId), now);
    }

    private List<EclassAssignment> search(BooleanExpression owner, LocalDateTime now, int limit) {
        return upcoming(owner, now)
                .select(assignment)
                .orderBy(assignment.dueAt.asc())
                .limit(limit)
                .fetch();
    }

    private long count(BooleanExpression owner, LocalDateTime now) {
        Long count = upcoming(owner, now)
                .select(assignment.count())
                .fetchOne();

        return Objects.requireNonNullElse(count, 0L);
    }

    private JPAQuery<?> upcoming(BooleanExpression owner, LocalDateTime now) {
        return queryFactory.from(assignment)
                .join(assignment.link, link)
                .join(link.device, device)
                .where(owner, link.status.eq(EclassLinkStatus.ACTIVE), isPending(), assignment.dueAt.gt(now));
    }

    // 아직 제출하지 않았고 학교에서 삭제되지도 않은 과제
    private BooleanExpression isPending() {
        return assignment.submitted.isFalse()
                .and(assignment.removedAt.isNull());
    }
}
