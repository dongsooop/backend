package com.dongsoop.dongsoop.notice.preference.entity;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "device_notice_preference")
public class DeviceNoticePreference {

    @Id
    @Column(name = "member_device_id")
    private Long memberDeviceId;

    // 기기 정리 스케줄러는 member_device 를 벌크 삭제하므로, DB 레벨에서 함께 지워지지 않으면 FK 위반으로 정리가 통째로 롤백된다
    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_device_id", foreignKey = @ForeignKey(
            name = "fk_device_notice_preference_member_device",
            foreignKeyDefinition = "FOREIGN KEY (member_device_id) REFERENCES member_device (id) ON DELETE CASCADE"))
    private MemberDevice device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    public DeviceNoticePreference(MemberDevice device, Department department) {
        this.device = device;
        this.department = department;
    }

    public void updateDepartment(Department department) {
        this.department = department;
    }
}
