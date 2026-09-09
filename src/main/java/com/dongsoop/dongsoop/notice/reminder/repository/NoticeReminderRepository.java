package com.dongsoop.dongsoop.notice.reminder.repository;

import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeReminderRepository
        extends JpaRepository<NoticeReminder, Long>, NoticeReminderRepositoryCustom {

    Optional<NoticeReminder> findByMemberIdAndNoticeDetailsId(Long memberId, Long noticeDetailsId);
}
