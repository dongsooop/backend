package com.dongsoop.dongsoop.notification.setting.dto;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.setting.entity.NotificationSetting;
import java.util.Objects;
import lombok.Getter;

@Getter
public abstract class SettingChanges {

    protected NotificationSetting setting;

    protected MemberDevice device;

    protected NotificationType type;

    protected boolean targetState;

    public SettingChanges(NotificationSetting setting, MemberDevice device, NotificationType type,
                          boolean targetState) {
        this.device = Objects.requireNonNull(device);
        this.type = Objects.requireNonNull(type);
        this.targetState = targetState;
        this.setting = Objects.requireNonNull(this.parseSetting(setting));
    }

    private NotificationSetting parseSetting(NotificationSetting setting) {
        if (setting == null) {
            return new NotificationSetting(this.device, this.type, this.targetState);
        }

        return setting;
    }

    protected abstract String getTypeName();

    public String getLog() {
        return this.getTypeName() + ": deviceToken:" + device.getDeviceToken() + ", notificationType:" + type
                + ", target state: " + targetState;
    }
}
