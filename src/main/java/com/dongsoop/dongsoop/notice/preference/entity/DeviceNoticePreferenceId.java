package com.dongsoop.dongsoop.notice.preference.entity;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceNoticePreferenceId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long device;

    @Enumerated(EnumType.STRING)
    private DepartmentType department;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceNoticePreferenceId that = (DeviceNoticePreferenceId) o;
        return Objects.equals(this.device, that.device)
                && Objects.equals(this.department, that.department);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.device, this.department);
    }
}
