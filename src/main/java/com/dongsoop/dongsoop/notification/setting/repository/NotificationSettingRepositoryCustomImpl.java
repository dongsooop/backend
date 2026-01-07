package com.dongsoop.dongsoop.notification.setting.repository;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationActiveStatus;
import com.dongsoop.dongsoop.notification.setting.entity.QNotificationSetting;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationSettingRepositoryCustomImpl implements NotificationSettingRepositoryCustom {

    private static final QNotificationSetting notificationSetting = QNotificationSetting.notificationSetting;

    private final JPAQueryFactory queryFactory;

    /**
     * 알림 설정 업데이트
     *
     * @param memberDevice 회원 디바이스
     * @param type         설정할 알림 타입
     * @param isEnabled    활성화 여부
     * @return 업데이트된 행의 수
     */
    @Override
    public long updateNotificationSetting(MemberDevice memberDevice, NotificationType type, boolean isEnabled) {
        return queryFactory.update(notificationSetting)
                .set(notificationSetting.enabled, isEnabled)
                .where(notificationSetting.id.device.eq(memberDevice)
                        .and(notificationSetting.id.notificationType.eq(type))
                        .and(notificationSetting.enabled.eq(!isEnabled)))
                .execute();
    }

    @Override
    public List<NotificationActiveStatus> findByDeviceToken(String deviceToken) {
        return queryFactory.select(Projections.constructor(
                        NotificationActiveStatus.class,
                        notificationSetting.id.notificationType,
                        notificationSetting.enabled))
                .from(notificationSetting)
                .where(notificationSetting.id.device.deviceToken.eq(deviceToken))
                .fetch();
    }
}
