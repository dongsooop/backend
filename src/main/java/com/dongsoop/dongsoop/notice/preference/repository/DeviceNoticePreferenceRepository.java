package com.dongsoop.dongsoop.notice.preference.repository;

import com.dongsoop.dongsoop.notice.preference.entity.DeviceNoticePreference;
import com.dongsoop.dongsoop.notice.preference.entity.DeviceNoticePreferenceId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceNoticePreferenceRepository extends
        JpaRepository<DeviceNoticePreference, DeviceNoticePreferenceId> {

    List<DeviceNoticePreference> findAllByIdDeviceId(Long memberDeviceId);
}
