package com.dongsoop.dongsoop.notice.repository;

import com.dongsoop.dongsoop.notice.dto.NoticeMaxIdByType;
import com.dongsoop.dongsoop.notice.entity.Notice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Query("SELECT n.id.department, MAX(n.id.noticeDetails.id) FROM Notice n GROUP BY n.id.department")
    List<NoticeMaxIdByType> findMaxIdGroupByType();
}
