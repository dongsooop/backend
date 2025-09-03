package com.dongsoop.dongsoop.notification.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.notification.dto.NotificationList;
import com.dongsoop.dongsoop.notification.dto.NotificationUnread;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.entity.QMemberNotification;
import com.dongsoop.dongsoop.notification.entity.QNotificationDetails;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final PageableUtil pageableUtil;

    private final QMember member = QMember.member;
    private final QMemberNotification memberNotice = QMemberNotification.memberNotification;
    private final QNotificationDetails notificationDetails = QNotificationDetails.notificationDetails;

    @Override
    public List<NotificationList> getMemberNotifications(Long memberId, Pageable pageable) {
        return queryFactory.select(Projections.constructor(NotificationList.class,
                        notificationDetails.id,
                        notificationDetails.title,
                        notificationDetails.body,
                        notificationDetails.type,
                        notificationDetails.value,
                        memberNotice.isRead,
                        notificationDetails.createdAt))
                .from(memberNotice)
                .innerJoin(memberNotice.id.details, notificationDetails)
                .where(memberNotice.id.member.id.eq(memberId)
                        .and(notificationDetails.isDeleted.eq(false)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), notificationDetails))
                .fetch();
    }

    @Override
    public long findUnreadCountByMemberId(Long memberId) {
        Long count = queryFactory.select(memberNotice.count())
                .from(memberNotice)
                .where(memberNotice.id.member.id.eq(memberId)
                        .and(memberNotice.isRead.eq(false))
                        .and(memberNotice.id.details.isDeleted.eq(false)))
                .fetchOne();

        if (count == null) {
            return 0L;
        }

        return count;
    }

    @Override
    public List<NotificationUnread> findUnreadCountByMemberIds(Collection<Long> memberIds) {
        return queryFactory.select(Projections.constructor(NotificationUnread.class,
                        member.id, memberNotice.count()))
                .from(member)
                .leftJoin(memberNotice)
                .on(member.eq(memberNotice.id.member)
                        .and(memberNotice.isRead.eq(false))
                        .and(memberNotice.id.details.isDeleted.eq(false)))
                .where(member.id.in(memberIds))
                .groupBy(member.id)
                .fetch();
    }

    @Override
    public Optional<MemberNotification> findByMemberIdAndNotificationId(Long memberId, Long notificationId) {
        MemberNotification result = queryFactory.selectFrom(memberNotice)
                .where(memberNotice.id.member.id.eq(memberId)
                        .and(memberNotice.id.details.id.eq(notificationId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public void updateAllAsRead(Long memberId) {
        queryFactory.update(memberNotice)
                .set(memberNotice.isRead, true)
                .where(memberNotice.id.member.id.eq(memberId)
                        .and(memberNotice.isRead.eq(false))
                        .and(memberNotice.id.details.isDeleted.eq(false)))
                .execute();
    }
}
