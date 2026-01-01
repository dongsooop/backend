package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.entity.QNotificationSetting;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
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
    public List<MemberDeviceDto> getAllMemberDevice() {
        return queryFactory.select(Projections.constructor(MemberDeviceDto.class,
                        memberDevice.member, memberDevice.deviceToken))
                .from(memberDevice)
                .fetch();
    }

    @Override
    public List<MemberDeviceDto> getMemberDeviceByDepartment(Department department) {
        return queryFactory.select(Projections.constructor(MemberDeviceDto.class,
                        memberDevice.member, memberDevice.deviceToken))
                .from(memberDevice)
                .where(memberDevice.member.department.eq(department))
                .fetch();
    }

    @Override
    public List<MemberDeviceDto> getMemberDeviceTokenByMemberIds(Collection<Long> memberIds) {
        return queryFactory.select(Projections.constructor(MemberDeviceDto.class,
                        member, memberDevice.deviceToken))
                .from(memberDevice)
                .innerJoin(memberDevice.member, member)
                .where(member.id.in(memberIds))
                .fetch();
    }

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
                .groupBy(member, memberDevice.deviceToken)
                .fetch();
    }

    @Override
    public List<MemberDeviceDto> getMemberDeviceTokenByMemberId(Long memberId) {
        return queryFactory.select(Projections.constructor(MemberDeviceDto.class,
                        memberDevice.member, memberDevice.deviceToken))
                .from(memberDevice)
                .where(memberDevice.member.id.eq(memberId))
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
        return notificationSetting.enabled.isTrue()
                .or(
                        // 또는 기본값이 활성화이면서
                        Expressions.asBoolean(isEnabledDefault)
                                // 저장된 설정이 없는 경우
                                .and(notificationSetting.id.device.isNull())
                );
    }

    private BooleanExpression notificationSettingEq(NotificationType notificationType) {
        return notificationSetting.id.device.eq(memberDevice)
                .and(notificationSetting.id.notificationType.eq(notificationType));
    }
}
