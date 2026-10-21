package com.dongsoop.dongsoop.notification.recipient.query;

import static com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice.memberDevice;
import static com.dongsoop.dongsoop.notification.setting.entity.QNotificationSetting.notificationSetting;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.querydsl.core.types.dsl.BooleanExpression;

public final class NotificationRecipientPredicates {
    private NotificationRecipientPredicates() {
        // Query adapters share one definition of push eligibility and setting defaults.
    }

    public static BooleanExpression hasPushToken() {
        return memberDevice.memberDeviceType.ne(MemberDeviceType.WEB)
                .and(memberDevice.deviceToken.isNotNull());
    }

    public static BooleanExpression matchesSetting(NotificationType type) {
        return notificationSetting.device.eq(memberDevice)
                .and(notificationSetting.id.notificationType.eq(type));
    }

    public static BooleanExpression isEnabled(NotificationType type) {
        if (!type.getDefaultActiveState()) {
            return notificationSetting.isNotNull().and(notificationSetting.enabled.isTrue());
        }
        return notificationSetting.isNull().or(notificationSetting.enabled.isTrue());
    }
}
