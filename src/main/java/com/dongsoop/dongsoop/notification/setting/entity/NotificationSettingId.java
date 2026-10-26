package com.dongsoop.dongsoop.notification.setting.entity;

import com.dongsoop.dongsoop.notification.constant.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "member_device_id", nullable = false, updatable = false)
    private Long deviceId;

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
        return Objects.equals(this.deviceId, that.deviceId)
                && Objects.equals(this.notificationType, that.notificationType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.deviceId, this.notificationType);
    }
}
