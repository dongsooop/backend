package com.dongsoop.dongsoop.notification.setting.entity;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class NotificationSetting {

    @EmbeddedId
    @Getter
    private NotificationSettingId id;

    @Column(nullable = false)
    private Boolean enabled;

    public NotificationSetting(MemberDevice device, NotificationType notificationType, boolean enabled) {
        this.id = new NotificationSettingId(device, notificationType);
        this.enabled = enabled;
    }

    public boolean isSameState(boolean enabled) {
        return this.enabled == enabled;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
