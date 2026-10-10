package com.dongsoop.dongsoop.notice.preference.entity;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원/비회원 구분 없이 기기 하나가 여러 학과의 공지 알림을 동시에 구독할 수 있도록,
 * (기기, 학과) 조합을 복합키로 하는 행 단위 구독으로 관리한다.
 */
@Entity
@IdClass(DeviceNoticePreferenceId.class)
@NoArgsConstructor
@Table(name = "device_notice_preference")
public class DeviceNoticePreference {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_device_id", nullable = false, updatable = false, foreignKey = @ForeignKey(
            name = "fk_device_notice_preference_member_device",
            foreignKeyDefinition = "FOREIGN KEY (member_device_id) REFERENCES member_device (id) ON DELETE CASCADE"))
    @Getter
    private MemberDevice device;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    @Getter
    private Department department;

    public DeviceNoticePreference(MemberDevice device, Department department) {
        this.device = device;
        this.department = department;
    }
}
