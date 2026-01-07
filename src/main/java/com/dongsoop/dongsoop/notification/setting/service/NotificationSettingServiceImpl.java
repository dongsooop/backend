package com.dongsoop.dongsoop.notification.setting.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationActiveStatus;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingFindRequest;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingRequest;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSettingId;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationSettingServiceImpl implements NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final MemberDeviceRepository memberDeviceRepository;

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
        updateEnable(request, false);

    }

    @Override
    @Transactional
    public void enableNotification(NotificationSettingRequest request) {
        updateEnable(request, true);
    }

    private void updateEnable(NotificationSettingRequest request, boolean isEnabled) {
        MemberDevice device = memberDeviceRepository.findByDeviceToken(request.deviceToken())
                .orElseThrow(UnregisteredDeviceException::new);

        NotificationSettingId id = new NotificationSettingId(device, request.notificationType());
        Optional<NotificationSetting> optionalNotificationSetting = this.notificationSettingRepository.findById(id);

        // 기본 정책과 동일하면 DB에서 제거
        if (request.notificationType().getDefaultActiveState() == isEnabled) {
            optionalNotificationSetting.ifPresent(notificationSettingRepository::delete);

            log.info(
                    "Notification setting matches default policy. Removed custom setting from DB. deviceToken: {}, notificationType: {}, isEnabled: {}",
                    request.deviceToken(), request.notificationType(), isEnabled);

            return;
        }

        // 업데이트 호출 후 업데이트 처리 내역이 존재한다면 종료
        if (optionalNotificationSetting.isEmpty()) {
            log.info("Notification setting not found. deviceToken: {}, notificationType: {}, isEnabled: {}",
                    request.deviceToken(), request.notificationType(), isEnabled);
            this.notificationSettingRepository.save(
                    new NotificationSetting(device, request.notificationType(), isEnabled));

            return;
        }

        // 기존 설정과 동일한 경우 업데이트하지 않음
        NotificationSetting notificationSetting = optionalNotificationSetting.get();
        if (notificationSetting.isSameState(isEnabled)) {
            log.info(
                    "Notification setting already in desired state. No update needed. deviceToken: {}, notificationType: {}, isEnabled: {}",
                    request.deviceToken(), request.notificationType(), isEnabled);
            return;
        }

        // 설정 업데이트
        notificationSetting.updateEnabled(isEnabled);
        log.info("Notification setting updated successfully. deviceToken: {}, notificationType: {}, isEnabled: {}",
                request.deviceToken(), request.notificationType(), isEnabled);
    }
}
