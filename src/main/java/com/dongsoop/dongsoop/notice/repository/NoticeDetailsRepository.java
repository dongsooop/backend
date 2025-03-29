package com.dongsoop.dongsoop.notice.repository;

import com.dongsoop.dongsoop.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeDetailsRepository extends JpaRepository<Notice, Long> {
}
