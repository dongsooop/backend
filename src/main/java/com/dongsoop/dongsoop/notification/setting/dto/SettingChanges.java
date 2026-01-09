package com.dongsoop.dongsoop.notification.setting.dto;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class SettingChanges {

    protected NotificationSetting setting;
    protected MemberDevice device;
    protected NotificationType type;
    protected Boolean targetState;

    protected abstract String getTypeName();

    public String getLog() {
        return this.getTypeName() + ": deviceToken:" + device.getDeviceToken() + ", notificationType:" + type
                + ", target state: " + targetState;
    }
}
