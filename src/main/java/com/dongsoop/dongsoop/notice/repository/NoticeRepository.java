package com.dongsoop.dongsoop.notice.repository;

import com.dongsoop.dongsoop.notice.dto.NoticeMaxIdByType;
import com.dongsoop.dongsoop.notice.entity.DepartmentNotice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoticeRepository extends JpaRepository<DepartmentNotice, Long> {

    @Query("SELECT n.id.type, MAX(n.id.notice.id) FROM DepartmentNotice n GROUP BY n.id.type")
    List<NoticeMaxIdByType> findMaxIdGroupByType();
}
