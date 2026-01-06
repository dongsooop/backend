package com.dongsoop.dongsoop.member.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.member.dto.DeleteMember;
import com.dongsoop.dongsoop.member.dto.LoginMemberDetails;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.entity.QMember;
import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.entity.QNotificationSetting;
import com.dongsoop.dongsoop.role.entity.QMemberRole;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    private static final String ROLE_DELIMITER = ",";

    private static final QMember member = QMember.member;
    private static final QMemberDevice memberDevice = QMemberDevice.memberDevice;
    private static final QMemberRole memberRole = QMemberRole.memberRole;
    private static final QNotificationSetting notificationSetting = QNotificationSetting.notificationSetting;

    private final JPAQueryFactory queryFactory;

    @Value("${member.nickname.alias.prefix:익명_}")
    private String nicknameAliasPrefix;

    @Override
    public Optional<LoginMemberDetails> findLoginMemberDetailById(Long id) {
        LoginMemberDetails loginMemberDetails = queryFactory.select(Projections.constructor(LoginMemberDetails.class,
                        member.id.as("id"),
                        member.nickname.as("nickname"),
                        member.email.as("email"),
                        member.department.id.as("departmentType"),
                        Expressions.stringTemplate("string_agg({0}, '" + ROLE_DELIMITER + "')", memberRole.id.role.roleType)))
                .from(member)
                .leftJoin(memberRole)
                .on(memberRole.id.member.eq(member))
                .where(eqId(id))
                .groupBy(member)
                .fetchOne();

        return Optional.ofNullable(loginMemberDetails);
    }

    @Override
    public long softDelete(DeleteMember deleteMember) {
        return queryFactory.update(member)
                .set(member.nickname, this.nicknameAliasPrefix + deleteMember.memberId())
                .set(member.password, deleteMember.passwordAlias())
                .setNull(member.studentId)
                .set(member.isDeleted, true)
                .set(member.updatedAt, LocalDateTime.now())
                .where(member.isDeleted.eq(false)
                        .and(member.id.eq(deleteMember.memberId())))
                .execute();
    }

    private BooleanExpression eqId(Long id) {
        if (id == null) {
            return null;
        }

        return QMember.member.id.eq(id);
    }

    @Override
    public List<Member> searchAllByDeviceNotEmpty() {
        boolean isEnabledDefault = NotificationType.NOTICE.getDefaultActiveState();

        // 알림 활성화 여부
        BooleanExpression enabledCondition = isEnableNotificationDevice(isEnabledDefault);

        return queryFactory.selectFrom(member)
                .innerJoin(memberDevice)
                .on(memberDevice.member.eq(member))
                .leftJoin(notificationSetting)
                .on(joinNotificationSetting(NotificationType.NOTICE))
                .where(member.isDeleted.isFalse() // 삭제되지 않은 회원
                        .and(enabledCondition)) // 알림을 활성화한 회원
                .distinct()
                .fetch();
    }

    @Override
    public List<Member> searchAllByDepartmentAndDeviceNotEmpty(Department department) {
        boolean isEnabledDefault = NotificationType.NOTICE.getDefaultActiveState();

        // 알림 활성화 여부
        BooleanExpression enabledCondition = isEnableNotificationDevice(isEnabledDefault);

        return queryFactory.selectFrom(member)
                .innerJoin(memberDevice)
                .on(memberDevice.member.eq(member))
                .leftJoin(notificationSetting)
                .on(joinNotificationSetting(NotificationType.NOTICE))
                .where(member.department.eq(department)
                        .and(member.isDeleted.isFalse()) // 삭제되지 않은 회원
                        .and(enabledCondition)) // 알림을 활성화한 회원
                .distinct()
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

    private BooleanExpression joinNotificationSetting(NotificationType notificationType) {
        return notificationSetting.id.device.eq(memberDevice) // 디바이스 조건 일치
                .and(notificationSetting.id.notificationType.eq(notificationType)); // 알림 타입 조건 일치
    }
}
