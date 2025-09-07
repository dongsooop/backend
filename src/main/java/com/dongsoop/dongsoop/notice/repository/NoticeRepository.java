package com.dongsoop.dongsoop.notice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.notice.dto.NoticeListResponse;
import com.dongsoop.dongsoop.notice.dto.NoticeRecentIdByDepartment;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.Notice.NoticeKey;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoticeRepository extends JpaRepository<Notice, NoticeKey>, NoticeRepositoryCustom {

    @Query("""
            SELECT d AS department, COALESCE(MAX(n.id.noticeDetails.id), 0) AS recentId
            FROM Department d
            LEFT JOIN Notice n ON n.id.department = d
            GROUP BY d
            """)
    List<NoticeRecentIdByDepartment> findRecentIdGroupByType();

    @Query("SELECT n.id.noticeDetails.id AS id,"
            + "n.id.noticeDetails.link AS link,"
            + "n.id.noticeDetails.createdAt AS createdAt,"
            + "n.id.noticeDetails.title AS title,"
            + "n.id.noticeDetails.writer AS writer "
            + "FROM Notice n "
            + "WHERE n.id.department = :department")
    Page<NoticeListResponse> findAllByDepartment(Department department, Pageable pageable);
}
