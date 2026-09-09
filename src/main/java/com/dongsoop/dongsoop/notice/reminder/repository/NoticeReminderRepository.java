package com.dongsoop.dongsoop.notice.reminder.repository;

import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface NoticeReminderRepository extends JpaRepository<NoticeReminder, Long> {

    Optional<NoticeReminder> findByMemberIdAndNoticeDetailsId(Long memberId, Long noticeDetailsId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from NoticeReminder r where r.id = :id")
    Optional<NoticeReminder> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select r.id
            from NoticeReminder r
            where r.status = :status
              and r.remindAt <= :until
            order by r.remindAt
            """)
    List<Long> findUpcomingIds(
            @Param("status") NoticeReminderStatus status,
            @Param("until") LocalDateTime until
    );

    @Query("""
            select r.id
            from NoticeReminder r
            where r.status = :status
              and r.remindAt <= :until
              and (r.claimedUntil is null or r.claimedUntil < :now)
            order by r.remindAt
            """)
    List<Long> findSchedulableIds(
            @Param("status") NoticeReminderStatus status,
            @Param("now") LocalDateTime now,
            @Param("until") LocalDateTime until
    );

    @Modifying
    @Transactional
    @Query("""
            update NoticeReminder r
               set r.claimedUntil = :claimedUntil
             where r.id = :id
               and r.status = :status
               and (r.claimedUntil is null or r.claimedUntil < :now)
            """)
    int claim(
            @Param("id") Long id,
            @Param("status") NoticeReminderStatus status,
            @Param("now") LocalDateTime now,
            @Param("claimedUntil") LocalDateTime claimedUntil
    );

    @Modifying
    @Transactional
    @Query("""
            update NoticeReminder r
               set r.claimedUntil = null,
                   r.retryCount = r.retryCount + 1,
                   r.status = case
                       when r.retryCount + 1 >= :maxRetryCount then :failed
                       else :pending
                   end
             where r.id = :id
               and r.status = :pending
            """)
    int releaseAfterFailure(
            @Param("id") Long id,
            @Param("maxRetryCount") int maxRetryCount,
            @Param("pending") NoticeReminderStatus pending,
            @Param("failed") NoticeReminderStatus failed
    );

    void deleteByMemberIdAndNoticeDetailsId(Long memberId, Long noticeDetailsId);
}
