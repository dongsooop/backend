package com.dongsoop.dongsoop.notification.setting.repository;

import com.dongsoop.dongsoop.notification.setting.dto.NotificationActiveStatus;
import java.util.List;

public interface NotificationSettingRepositoryCustom {

    List<NotificationActiveStatus> findByDeviceToken(String deviceToken);
}
