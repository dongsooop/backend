package com.dongsoop.dongsoop.notification.setting.dto;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;

public class SettingUpdate extends SettingChanges {

    public SettingUpdate(NotificationSetting setting, MemberDevice device, NotificationType type, boolean targetState) {
        super(setting, device, type, targetState);
    }

    @Override
    public String getTypeName() {
        return "Updated";
    }
}
