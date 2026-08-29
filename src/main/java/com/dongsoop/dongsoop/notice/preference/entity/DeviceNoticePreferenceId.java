package com.dongsoop.dongsoop.notice.preference.entity;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceNoticePreferenceId {

    // 기기 정리 스케줄러는 member_device 를 벌크 삭제하므로, DB 레벨에서 함께 지워지지 않으면 FK 위반으로 정리가 통째로 롤백된다
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_device_id", nullable = false, updatable = false, foreignKey = @ForeignKey(
            name = "fk_device_notice_preference_member_device",
            foreignKeyDefinition = "FOREIGN KEY (member_device_id) REFERENCES member_device (id) ON DELETE CASCADE"))
    private MemberDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    private Department department;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceNoticePreferenceId that = (DeviceNoticePreferenceId) o;
        return Objects.equals(this.device.getId(), that.device.getId())
                && Objects.equals(this.department.getId(), that.department.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.device.getId(), this.department.getId());
    }
}
