package com.dongsoop.dongsoop.notice.preference.repository;

import com.dongsoop.dongsoop.notice.preference.entity.DeviceNoticePreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceNoticePreferenceRepository extends JpaRepository<DeviceNoticePreference, Long> {

    Optional<DeviceNoticePreference> findByMemberDeviceId(Long memberDeviceId);
}
