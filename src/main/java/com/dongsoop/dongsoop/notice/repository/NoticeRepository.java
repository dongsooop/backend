package com.dongsoop.dongsoop.notice.repository;

import com.dongsoop.dongsoop.department.entity.Department;
import com.dongsoop.dongsoop.notice.dto.NoticeListResponse;
import com.dongsoop.dongsoop.notice.dto.NoticeMaxIdByType;
import com.dongsoop.dongsoop.notice.entity.Notice;
import com.dongsoop.dongsoop.notice.entity.Notice.NoticeKey;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NoticeRepository extends JpaRepository<Notice, NoticeKey> {

    @Query("SELECT n.id.department AS department, MAX(n.id.noticeDetails.id) AS maxId FROM Notice n GROUP BY n.id.department")
    List<NoticeMaxIdByType> findMaxIdGroupByType();

    @Query("SELECT n.id.noticeDetails.id AS id,"
            + "n.id.noticeDetails.link AS link,"
            + "n.id.noticeDetails.createdAt AS createdAt,"
            + "n.id.noticeDetails.title AS title,"
            + "n.id.noticeDetails.writer AS writer "
            + "FROM Notice n "
            + "WHERE n.id.department = :department")
    Page<NoticeListResponse> findAllByDepartment(Department department, Pageable pageable);
}
