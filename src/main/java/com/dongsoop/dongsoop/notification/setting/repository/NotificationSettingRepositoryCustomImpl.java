package com.dongsoop.dongsoop.notification.setting.repository;

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
