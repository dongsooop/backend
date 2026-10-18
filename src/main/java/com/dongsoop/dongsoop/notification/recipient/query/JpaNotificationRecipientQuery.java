package com.dongsoop.dongsoop.notification.recipient.query;

import static com.dongsoop.dongsoop.member.entity.QMember.member;
import static com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice.memberDevice;
import static com.dongsoop.dongsoop.notification.setting.entity.QNotificationSetting.notificationSetting;
import static com.dongsoop.dongsoop.role.entity.QMemberRole.memberRole;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.role.entity.RoleType;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaNotificationRecipientQuery implements NotificationRecipientQuery {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberDeviceDto> findDevicesWithNotification(MemberDeviceFindCondition condition) {
        return queryFactory.select(Projections.constructor(MemberDeviceDto.class, member, memberDevice.deviceToken))
                .from(memberDevice)
                .innerJoin(memberDevice.member, member)
                .leftJoin(notificationSetting)
                .on(NotificationRecipientPredicates.matchesSetting(condition.notificationType()))
                .where(member.id.in(condition.memberIds()), NotificationRecipientPredicates.hasPushToken(),
                        NotificationRecipientPredicates.isEnabled(condition.notificationType()))
                .distinct()
                .fetch();
    }

    @Override
    public List<Member> findMembersByRoleWithDevice(RoleType roleType) {
        return queryFactory.selectFrom(member)
                .innerJoin(memberDevice)
                .on(memberDevice.member.eq(member))
                .innerJoin(memberRole)
                .on(memberRole.member.eq(member))
                .where(memberRole.role.roleType.eq(roleType)
                        .and(member.isDeleted.isFalse()))
                .distinct()
                .fetch();
    }

}
