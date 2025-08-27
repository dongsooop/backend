package com.dongsoop.dongsoop.notification.repository;

import com.dongsoop.dongsoop.common.PageableUtil;
import com.dongsoop.dongsoop.notification.entity.NotificationDetails;
import com.dongsoop.dongsoop.notification.entity.QMemberNotification;
import com.dongsoop.dongsoop.notification.entity.QNotificationDetails;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryCustomImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final PageableUtil pageableUtil;

    private final QMemberNotification memberNotice = QMemberNotification.memberNotification;
    private final QNotificationDetails notificationDetails = QNotificationDetails.notificationDetails;

    @Override
    public List<NotificationDetails> getMemberNotifications(Long memberId, Pageable pageable) {
        return queryFactory.select(notificationDetails)
                .from(memberNotice)
                .leftJoin(memberNotice.id.details, notificationDetails)
                .where(memberNotice.id.member.id.eq(memberId))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(pageableUtil.getAllOrderSpecifiers(pageable.getSort(), notificationDetails))
                .fetch();
    }
}
