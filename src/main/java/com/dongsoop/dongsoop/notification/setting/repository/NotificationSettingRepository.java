package com.dongsoop.dongsoop.notification.setting.repository;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSettingId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, NotificationSettingId>,
        NotificationSettingRepositoryCustom {

    Optional<NotificationSetting> findByIdDeviceIdAndIdNotificationType(Long deviceId, NotificationType notificationType);
}
