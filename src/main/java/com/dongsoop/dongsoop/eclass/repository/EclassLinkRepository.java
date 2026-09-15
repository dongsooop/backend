package com.dongsoop.dongsoop.eclass.repository;

import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface EclassLinkRepository extends JpaRepository<EclassLink, Long> {

    /**
     * 알림 발송이 트랜잭션 밖에서 기기를 읽으므로 지연 프록시를 남기지 않는다.
     */
    @Query("select l from EclassLink l join fetch l.device d left join fetch d.member where d.id = :deviceId")
    Optional<EclassLink> findByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 스케줄러가 트랜잭션 밖에서 기기·회원 정보를 읽으므로 fetch join으로 미리 가져온다.
     */
    @Query("select l from EclassLink l join fetch l.device d left join fetch d.member where l.status = :status")
    List<EclassLink> findAllByStatus(@Param("status") EclassLinkStatus status);

    boolean existsByDeviceMemberIdAndStatus(Long memberId, EclassLinkStatus status);

    /**
     * 수동 동기화 쿨다운 검사와 시각 기록을 UPDATE 한 번으로 묶는다. 같은 기기의 동시 요청은 먼저 갱신한 하나만
     * 1을 받고 나머지는 0을 받아, 학교 서버 호출이 쿨다운마다 한 번으로 묶인다.
     *
     * @return 갱신된 행 수. 0이면 쿨다운 중이다
     */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update EclassLink l set l.lastManualSyncAt = :now "
            + "where l.id = :id and (l.lastManualSyncAt is null or l.lastManualSyncAt <= :threshold)")
    int claimManualSync(@Param("id") Long id, @Param("now") LocalDateTime now,
                        @Param("threshold") LocalDateTime threshold);
}
