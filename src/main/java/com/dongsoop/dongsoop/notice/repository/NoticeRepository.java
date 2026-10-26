package com.dongsoop.dongsoop.notice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.notice.dto.NoticeListResponse;
import com.dongsoop.dongsoop.notice.dto.NoticeRecentIdByDepartment;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.Notice.NoticeKey;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoticeRepository extends JpaRepository<Notice, NoticeKey>, NoticeRepositoryCustom {

    @Query("""
            SELECT d AS department, COALESCE(MAX(n.noticeDetails.id), 0) AS recentId
            FROM Department d
            LEFT JOIN Notice n ON n.department = d
            GROUP BY d
            """)
    List<NoticeRecentIdByDepartment> findRecentIdGroupByType();

    @Query("SELECT n.noticeDetails.id AS id,"
            + "n.noticeDetails.link AS link,"
            + "n.noticeDetails.createdAt AS createdAt,"
            + "n.noticeDetails.title AS title,"
            + "n.noticeDetails.writer AS writer "
            + "FROM Notice n "
            + "WHERE n.department = :department "
            + "AND n.deletedAt IS NULL")
    Page<NoticeListResponse> findAllByDepartment(Department department, Pageable pageable);

    @Query("SELECT n.noticeDetails.id AS id,"
            + "n.noticeDetails.link AS link,"
            + "n.noticeDetails.createdAt AS createdAt,"
            + "n.noticeDetails.title AS title,"
            + "n.noticeDetails.writer AS writer "
            + "FROM Notice n "
            + "WHERE n.department IN :departments "
            + "AND n.deletedAt IS NULL "
            + "ORDER BY n.noticeDetails.id DESC")
    Page<NoticeListResponse> findAllByDepartmentIn(Collection<Department> departments, Pageable pageable);
}
