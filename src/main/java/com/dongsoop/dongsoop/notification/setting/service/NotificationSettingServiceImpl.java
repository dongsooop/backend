package com.dongsoop.dongsoop.notification.setting.service;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationActiveStatus;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingFindRequest;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingRequest;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingUpdate;
import com.dongsoop.dongsoop.notification.setting.dto.RecruitmentSettingUpdateRequest;
import com.dongsoop.dongsoop.notification.setting.repository.NotificationSettingRepository;
import com.dongsoop.dongsoop.notification.setting.service.switcher.NotificationSettingSwitcher;
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
    private final NotificationSettingSwitcher settingSwitcher;

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
        NotificationSettingUpdate update = new NotificationSettingUpdate(request.deviceToken(),
                List.of(request.notificationType()), false);
        settingSwitcher.updateStatus(update);
    }

    @Override
    @Transactional
    public void enableNotification(NotificationSettingRequest request) {
        NotificationSettingUpdate update = new NotificationSettingUpdate(request.deviceToken(),
                List.of(request.notificationType()), true);
        settingSwitcher.updateStatus(update);
    }

    @Override
    @Transactional
    public void updateRecruitmentApplyNotification(RecruitmentSettingUpdateRequest request) {
        NotificationSettingUpdate update = new NotificationSettingUpdate(request.deviceToken(),
                List.of(
                        NotificationType.RECRUITMENT_PROJECT_APPLY,
                        NotificationType.RECRUITMENT_TUTORING_APPLY,
                        NotificationType.RECRUITMENT_STUDY_APPLY
                ),
                request.targetState());

        settingSwitcher.updateStatus(update);
    }

    @Override
    @Transactional
    public void updateRecruitmentResultNotification(RecruitmentSettingUpdateRequest request) {
        NotificationSettingUpdate update = new NotificationSettingUpdate(request.deviceToken(),
                List.of(
                        NotificationType.RECRUITMENT_PROJECT_APPLY_RESULT,
                        NotificationType.RECRUITMENT_TUTORING_APPLY_RESULT,
                        NotificationType.RECRUITMENT_STUDY_APPLY_RESULT
                ),
                request.targetState());

        settingSwitcher.updateStatus(update);
    }
}
