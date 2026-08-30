package com.dongsoop.dongsoop.notice.preference.entity;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원/비회원 구분 없이 기기 하나가 여러 학과의 공지 알림을 동시에 구독할 수 있도록,
 * (기기, 학과) 조합을 복합키로 하는 행 단위 구독으로 관리한다.
 */
@Entity
@NoArgsConstructor
@Table(name = "device_notice_preference")
public class DeviceNoticePreference {

    @EmbeddedId
    @Getter
    private DeviceNoticePreferenceId id;

    public DeviceNoticePreference(MemberDevice device, Department department) {
        this.id = new DeviceNoticePreferenceId(device, department);
    }
}
