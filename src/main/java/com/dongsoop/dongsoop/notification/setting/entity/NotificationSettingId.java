package com.dongsoop.dongsoop.notification.setting.entity;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingId {

    @JoinColumn(name = "member_device_id", nullable = false, updatable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private MemberDevice device;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, updatable = false)
    private NotificationType notificationType;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotificationSettingId that = (NotificationSettingId) o;
        return Objects.equals(this.device.getDeviceToken(), that.device.getDeviceToken())
                && Objects.equals(this.notificationType, that.notificationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.device.getDeviceToken(), this.notificationType);
    }
}
