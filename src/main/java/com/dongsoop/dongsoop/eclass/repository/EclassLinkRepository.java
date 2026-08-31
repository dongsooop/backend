package com.dongsoop.dongsoop.eclass.repository;

import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EclassLinkRepository extends JpaRepository<EclassLink, Long> {

    Optional<EclassLink> findByDeviceId(Long deviceId);

    /**
     * 스케줄러가 트랜잭션 밖에서 기기·회원 정보를 읽으므로 fetch join으로 미리 가져온다.
     */
    @Query("select l from EclassLink l join fetch l.device d left join fetch d.member where l.status = :status")
    List<EclassLink> findAllByStatus(@Param("status") EclassLinkStatus status);

    @Query("select l from EclassLink l join fetch l.device d where d.member.id = :memberId and l.status = :status")
    List<EclassLink> findAllByMemberIdAndStatus(@Param("memberId") Long memberId,
                                                @Param("status") EclassLinkStatus status);
}
