package com.dongsoop.dongsoop.notification.setting.entity;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
public class NotificationSetting {

    @EmbeddedId
    @Getter
    private NotificationSettingId id;

    @MapsId("deviceId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_device_id", nullable = false, updatable = false)
    private MemberDevice device;

    @Getter
    @Column(nullable = false)
    private Boolean enabled;

    public NotificationSetting(MemberDevice device, NotificationType notificationType, boolean enabled) {
        this.id = new NotificationSettingId(device.getId(), notificationType);
        this.device = device;
        this.enabled = enabled;
    }

    public boolean isSameState(boolean enabled) {
        return this.enabled == enabled;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
