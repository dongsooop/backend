package com.dongsoop.dongsoop.memberdevice.repository;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberDeviceRepository extends JpaRepository<MemberDevice, Long>, MemberDeviceRepositoryCustom {

    boolean existsByDeviceToken(String deviceToken);

    Optional<MemberDevice> findByMemberIdAndDeviceToken(Long memberId, String deviceToken);

    Optional<MemberDevice> findByDeviceToken(String deviceToken);

    Optional<MemberDevice> findByFid(String fid);

    void deleteByDeviceToken(String deviceToken);

    List<MemberDevice> findByMemberId(Long memberId);
}
