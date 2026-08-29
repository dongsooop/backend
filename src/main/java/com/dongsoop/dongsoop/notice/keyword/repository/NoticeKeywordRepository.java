package com.dongsoop.dongsoop.notice.keyword.repository;

import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeyword;
import com.dongsoop.dongsoop.notice.keyword.entity.NoticeKeywordType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeKeywordRepository extends JpaRepository<NoticeKeyword, Long> {

    List<NoticeKeyword> findAllByMemberId(Long memberId);

    List<NoticeKeyword> findAllByMemberIdIn(Collection<Long> memberIds);

    boolean existsByMemberIdAndKeywordAndType(Long memberId, String keyword, NoticeKeywordType type);

    Optional<NoticeKeyword> findByIdAndMemberId(Long id, Long memberId);
}