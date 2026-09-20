package com.dongsoop.dongsoop.notification.repository;

import static com.dongsoop.dongsoop.notification.entity.QNotificationDetails.notificationDetails;
import static com.dongsoop.dongsoop.notification.entity.QMemberNotification.memberNotification;
import static com.dongsoop.dongsoop.member.entity.QMember.member;
import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.notification.dto.NotificationList;
import com.dongsoop.dongsoop.notification.dto.NotificationUnread;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
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


    @Override
    public List<NotificationList> getMemberNotifications(Long memberId, Pageable pageable) {
        return queryFactory.select(Projections.constructor(NotificationList.class,
                        notificationDetails.id,
                        notificationDetails.title,
                        notificationDetails.body,
                        notificationDetails.type,
                        notificationDetails.value,
                        memberNotification.isRead,
                        notificationDetails.createdAt))
                .from(memberNotification)
                .innerJoin(memberNotification.id.details, notificationDetails)
                .where(memberNotification.id.member.id.eq(memberId)
                        .and(notificationDetails.isDeleted.eq(false)))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), notificationDetails))
                .fetch();
    }

    @Override
    public int findUnreadCountByMemberId(Long memberId) {
        Long count = queryFactory.select(memberNotification.count())
                .from(memberNotification)
                .where(memberNotification.id.member.id.eq(memberId)
                        .and(memberNotification.isRead.eq(false))
                        .and(memberNotification.id.details.isDeleted.eq(false)))
                .fetchOne();

        if (count == null) {
            return 0;
        }

        if (count > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return Math.toIntExact(count);
    }

    @Override
    public List<NotificationUnread> findUnreadCountByMemberIds(Collection<Long> memberIds) {
        return queryFactory.select(Projections.constructor(NotificationUnread.class,
                        member.id, memberNotification.count()))
                .from(member)
                .leftJoin(memberNotification)
                .on(member.eq(memberNotification.id.member)
                        .and(memberNotification.isRead.eq(false))
                        .and(memberNotification.id.details.isDeleted.eq(false)))
                .where(member.id.in(memberIds))
                .groupBy(member.id)
                .fetch();
    }

    @Override
    public Optional<MemberNotification> findByMemberIdAndNotificationId(Long memberId, Long notificationId) {
        MemberNotification result = queryFactory.selectFrom(memberNotification)
                .where(memberNotification.id.member.id.eq(memberId)
                        .and(memberNotification.id.details.id.eq(notificationId)))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public void updateAllAsRead(Long memberId) {
        queryFactory.update(memberNotification)
                .set(memberNotification.isRead, true)
                .where(memberNotification.id.member.id.eq(memberId)
                        .and(memberNotification.isRead.eq(false))
                        .and(memberNotification.id.details.isDeleted.eq(false)))
                .execute();
    }
}
