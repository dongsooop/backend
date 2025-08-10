package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberDeviceRepository extends JpaRepository<MemberDevice, Long> {
    boolean existsByDeviceToken(String deviceToken);

    Optional<MemberDevice> findByMemberIdAndDeviceToken(Long memberId, String deviceToken);
}
