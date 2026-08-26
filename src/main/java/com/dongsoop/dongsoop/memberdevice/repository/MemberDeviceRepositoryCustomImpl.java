package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceDto;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceFindCondition;
import com.dongsoop.dongsoop.memberdevice.dto.MemberDeviceResponse;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.dongsoop.dongsoop.notice.preference.entity.QDeviceNoticePreference;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.entity.QNotificationSetting;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAQueryFactory;
import io.micrometer.common.util.StringUtils;
import java.time.LocalDateTime;
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
    private final QDeviceNoticePreference deviceNoticePreference =
            QDeviceNoticePreference.deviceNoticePreference;

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
                        .and(isNotWebDevice())
                        .and(memberDevice.deviceToken.isNotNull())
                        .and(enabledCondition)) // 알림 활성화 조건
                .distinct()
                .fetch();
    }

    @Override
    public List<String> getDeviceByMemberId(Long memberId) {
        return queryFactory.select(memberDevice.deviceToken)
                .from(memberDevice)
                .where(memberDevice.member.id.eq(memberId)
                        .and(isNotWebDevice())
                        .and(memberDevice.deviceToken.isNotNull()))
                .fetch();
    }

    @Override
    public List<MemberDevice> searchGuestDevicesByDepartment(DepartmentType departmentType) {
        boolean isEnabledDefault = NotificationType.NOTICE.getDefaultActiveState();

        return queryFactory.selectFrom(memberDevice)
                .leftJoin(notificationSetting)
                .on(notificationSettingEq(NotificationType.NOTICE))
                .leftJoin(deviceNoticePreference)
                .on(deviceNoticePreference.memberDeviceId.eq(memberDevice.id))
                .where(memberDevice.member.isNull() // 비회원
                        .and(memberDevice.deviceToken.isNotNull())
                        .and(isNotWebDevice())
                        .and(guestDepartmentEq(departmentType))
                        .and(isEnableNotificationDevice(isEnabledDefault)))
                .distinct()
                .fetch();
    }

    @Override
    public List<MemberDeviceResponse> findDeviceListByMemberId(Long memberId, String currentDeviceToken) {
        Expression<Boolean> isCurrentDevice = isCurrentDevice(currentDeviceToken);

        return queryFactory.select(Projections.constructor(MemberDeviceResponse.class,
                        memberDevice.id,
                        memberDevice.memberDeviceType,
                        isCurrentDevice,
                        memberDevice.updatedAt))
                .from(memberDevice)
                .where(memberDevice.member.id.eq(memberId))
                .fetch();
    }

    private Expression<Boolean> isCurrentDevice(String currentDeviceToken) {
        if (StringUtils.isBlank(currentDeviceToken)) {
            return Expressions.FALSE;
        }

        return memberDevice.deviceToken.eq(currentDeviceToken);
    }

    private BooleanExpression isNotWebDevice() {
        return memberDevice.memberDeviceType.ne(MemberDeviceType.WEB);
    }

    private BooleanExpression isEnableNotificationDevice(boolean isEnabledDefault) {
        // 기본 설정이 비활성화인 경우
        if (!isEnabledDefault) {
            // 저장된 알림이 활성화 상태인지 검증
            return notificationSetting.isNotNull()
                    .and(notificationSetting.enabled.isTrue());
        }

        // 기본이 활성화인 경우
        return notificationSetting.isNull()
                .or(notificationSetting.enabled.isTrue());
    }

    private BooleanExpression notificationSettingEq(NotificationType notificationType) {
        return notificationSetting.id.device.eq(memberDevice)
                .and(notificationSetting.id.notificationType.eq(notificationType));
    }

    /**
     * 대학 공지(DEPT_1001)는 학과 설정 여부와 무관하게 전체 비회원이 대상이므로 조건을 걸지 않는다.
     */
    private BooleanExpression guestDepartmentEq(DepartmentType departmentType) {
        if (departmentType == DepartmentType.DEPT_1001) {
            return null;
        }

        return deviceNoticePreference.department.id.eq(departmentType);
    }

    @Override
    public long deleteExpiredDevices(LocalDateTime cutoff) {
        return queryFactory.delete(memberDevice)
                .where(memberDevice.lastAccess.lt(cutoff)
                        .and(memberDevice.memberDeviceType.eq(MemberDeviceType.WEB)
                                .or(memberDevice.deviceToken.isNull())))
                .execute();
    }
}
