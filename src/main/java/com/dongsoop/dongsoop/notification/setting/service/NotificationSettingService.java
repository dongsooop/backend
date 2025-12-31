package com.dongsoop.dongsoop.notification.setting.service;

import com.dongsoop.dongsoop.notification.setting.dto.NotificationSettingRequest;

public interface NotificationSettingService {

    void disableNotification(NotificationSettingRequest request);

    void enableNotification(NotificationSettingRequest request);
}
