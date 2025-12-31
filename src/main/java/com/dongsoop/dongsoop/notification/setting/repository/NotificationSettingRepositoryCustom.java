package com.dongsoop.dongsoop.notification.setting.repository;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;

public interface NotificationSettingRepositoryCustom {

    long updateNotificationSetting(MemberDevice memberDevice, NotificationType type, boolean isEnabled);
}
