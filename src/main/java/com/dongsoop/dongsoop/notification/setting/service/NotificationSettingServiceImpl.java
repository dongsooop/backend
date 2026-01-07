package com.dongsoop.dongsoop.notification.setting.service;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationActiveStatus;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingFindRequest;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingRequest;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import com.dongsoop.dongsoop.notification.setting.service.switcher.NotificationSettingDisable;
import com.dongsoop.dongsoop.notification.setting.service.switcher.NotificationSettingEnable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSettingServiceImpl implements NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationSettingEnable notificationSettingEnable;
    private final NotificationSettingDisable notificationSettingDisable;

    @Override
    public Map<NotificationType, Boolean> getNotificationSettings(NotificationSettingFindRequest request) {
        List<NotificationActiveStatus> notificationActiveStatuses = notificationSettingRepository.findByDeviceToken(
                request.deviceToken());

        Map<NotificationType, Boolean> settingMap = notificationActiveStatuses.stream()
                .collect(HashMap::new,
                        (map, status) -> map.put(status.notificationType(), status.enabled()),
                        HashMap::putAll);

        for (NotificationType type : NotificationType.values()) {
            if (!settingMap.containsKey(type)) {
                settingMap.put(type, type.getDefaultActiveState());
            }
        }

        return settingMap;
    }

    @Override
    @Transactional
    public void disableNotification(NotificationSettingRequest request) {
        notificationSettingDisable.updateStatus(request);
    }

    @Override
    @Transactional
    public void enableNotification(NotificationSettingRequest request) {
        notificationSettingEnable.updateStatus(request);
    }
}
