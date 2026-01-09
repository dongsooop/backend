package com.dongsoop.dongsoop.notification.setting.service;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingFindRequest;
import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingRequest;
import com.dongsoop.dongsoop.notification.setting.dto.RecruitmentSettingUpdateRequest;
import java.util.Map;

public interface NotificationSettingService {

    Map<NotificationType, Boolean> getNotificationSettings(NotificationSettingFindRequest request);

    void disableNotification(NotificationSettingRequest request);

    void enableNotification(NotificationSettingRequest request);

    void updateRecruitmentApplyNotification(RecruitmentSettingUpdateRequest request);

    void updateRecruitmentResultNotification(RecruitmentSettingUpdateRequest request);
}
