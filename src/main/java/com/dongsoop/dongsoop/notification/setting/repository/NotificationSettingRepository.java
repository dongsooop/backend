package com.dongsoop.dongsoop.notification.setting.repository;

import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSettingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, NotificationSettingId>,
        NotificationSettingRepositoryCustom {
}
