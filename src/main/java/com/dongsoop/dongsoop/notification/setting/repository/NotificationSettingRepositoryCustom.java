package com.dongsoop.dongsoop.notification.setting.repository;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationActiveStatus;
import java.util.List;

public interface NotificationSettingRepositoryCustom {

    long updateNotificationSetting(MemberDevice memberDevice, NotificationType type, boolean isEnabled);

    List<NotificationActiveStatus> findByDeviceToken(String deviceToken);
}
