package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.entity.QNotificationSetting;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberDeviceRepositoryCustomImpl implements MemberDeviceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QNotificationSetting notificationSetting = QNotificationSetting.notificationSetting;
    private final QMemberDevice memberDevice = QMemberDevice.memberDevice;
    private final QMember member = QMember.member;

    @Override
    public List<MemberDeviceDto> findDevicesWithNotification(MemberDeviceFindCondition condition) {
        boolean isEnabledDefault = condition.notificationType().getDefaultActiveState();

        // 알림 활성화 여부
        BooleanExpression enabledCondition = isEnableNotificationDevice(isEnabledDefault);

        return queryFactory.select(Projections.constructor(MemberDeviceDto.class,
                        member, memberDevice.deviceToken))
                .from(memberDevice)
                .innerJoin(memberDevice.member, member)
                .leftJoin(notificationSetting)
                .on(notificationSettingEq(condition.notificationType())) // 디바이스 및 알림 타입 조건 일치
                .where(member.id.in(condition.memberIds()) // memberIds 조건
                        .and(enabledCondition)) // 알림 활성화 조건
                .distinct()
                .fetch();
    }

    @Override
    public List<String> getDeviceByMemberId(Long memberId) {
        return queryFactory.select(memberDevice.deviceToken)
                .from(memberDevice)
                .where(memberDevice.member.id.eq(memberId))
                .fetch();
    }

    private BooleanExpression isEnableNotificationDevice(boolean isEnabledDefault) {
        // 기본 설정이 비활성화인 경우
        if (!isEnabledDefault) {
            // 저장된 알림이 활성화 상태인지 검증
            return notificationSetting.enabled.isTrue();
        }

        // 기본이 활성화인 경우
        return notificationSetting.isNull()
                .or(notificationSetting.enabled.isTrue());
    }

    private BooleanExpression notificationSettingEq(NotificationType notificationType) {
        return notificationSetting.id.device.eq(memberDevice)
                .and(notificationSetting.id.notificationType.eq(notificationType));
    }
}
