package com.dongsoop.dongsoop.notice.keyword.repository;

import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeyword;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeywordType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeKeywordRepository extends JpaRepository<NoticeKeyword, Long> {

    List<NoticeKeyword> findAllByDeviceId(Long deviceId);

    List<NoticeKeyword> findAllByDeviceIdIn(Collection<Long> deviceIds);

    boolean existsByDeviceIdAndKeywordAndType(Long deviceId, String keyword, NoticeKeywordType type);

    Optional<NoticeKeyword> findByIdAndDeviceIdIn(Long id, Collection<Long> deviceIds);

    void deleteByDeviceIdInAndKeywordAndType(Collection<Long> deviceIds, String keyword, NoticeKeywordType type);
}