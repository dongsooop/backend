package com.dongsoop.dongsoop.notice.notification.query;

import static com.dongsoop.dongsoop.member.entity.QMember.member;
import static com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice.memberDevice;
import static com.dongsoop.dongsoop.notification.setting.entity.QNotificationSetting.notificationSetting;
import static com.dongsoop.dongsoop.notice.preference.entity.QDeviceNoticePreference.deviceNoticePreference;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.memberdevice.dto.DeviceSubscription;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.recipient.query.NotificationRecipientPredicates;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaNoticeNotificationTargetQuery implements NoticeNotificationTargetQuery {
    private final JPAQueryFactory queryFactory;

    @Override
    public List<MemberDevice> searchDevicesByDepartments(Collection<DepartmentType> departmentTypes) {
        return queryFactory.selectFrom(memberDevice)
                .leftJoin(memberDevice.member, member).fetchJoin()
                .leftJoin(notificationSetting)
                .on(NotificationRecipientPredicates.matchesSetting(NotificationType.NOTICE))
                .leftJoin(deviceNoticePreference)
                .on(deviceNoticePreference.device.id.eq(memberDevice.id))
                .where(NotificationRecipientPredicates.hasPushToken()
                        .and(departmentPreferenceIn(departmentTypes))
                        .and(NotificationRecipientPredicates.isEnabled(NotificationType.NOTICE)))
                .distinct()
                .fetch();
    }

    @Override
    public List<DeviceSubscription> findSubscriptionsByDeviceIds(Collection<Long> deviceIds) {
        if (deviceIds.isEmpty()) {
            return List.of();
        }

        return queryFactory.select(Projections.constructor(DeviceSubscription.class,
                        deviceNoticePreference.device.id,
                        deviceNoticePreference.department.id))
                .from(deviceNoticePreference)
                .where(deviceNoticePreference.device.id.in(deviceIds))
                .fetch();
    }

    // 대학 공지는 학과 구독 여부와 무관하게 모든 적격 기기가 대상이다.
    private BooleanExpression departmentPreferenceIn(Collection<DepartmentType> departmentTypes) {
        boolean hasAllDepartment = departmentTypes.stream()
                .anyMatch(DepartmentType::isAllDepartment);

        if (hasAllDepartment) {
            return null;
        }

        return deviceNoticePreference.department.id.in(departmentTypes);
    }

}
