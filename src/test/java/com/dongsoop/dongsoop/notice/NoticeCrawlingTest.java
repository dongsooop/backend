package com.dongsoop.dongsoop.notice;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import com.dongsoop.dongsoop.notice.repository.NoticeRepository;
import com.dongsoop.dongsoop.notice.service.NoticeScheduler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NoticeCrawlingTest {

    @Autowired
    NoticeScheduler noticeScheduler;

    @Autowired
    NoticeRepository noticeRepository;

    @Autowired
    NoticeDetailsRepository noticeDetailsRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void cleanup() {
        noticeRepository.deleteAll();
        noticeDetailsRepository.deleteAll();
    }

    @Test
    void get_at_least_one_notice_from_each_department() {
        noticeScheduler.scheduled();

        String jpql = "SELECT COUNT(d.id) " +
                "FROM Department d " +
                "LEFT JOIN Notice n ON n.id.department = d " +
                "WHERE n.id.noticeDetails.id IS NULL ";

        Query query = entityManager.createQuery(jpql);
        Integer result = query.getFirstResult();

        assertThat(result).isZero();
    }
}
