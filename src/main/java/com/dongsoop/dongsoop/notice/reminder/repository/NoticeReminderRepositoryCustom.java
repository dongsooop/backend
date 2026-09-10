package com.dongsoop.dongsoop.notice.reminder.repository;

import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminderStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoticeReminderRepositoryCustom {

    Optional<NoticeReminder> findByIdForUpdate(Long id);

    Optional<NoticeReminder> findByDeviceIdAndNoticeDetailsIdForUpdate(Long deviceId, Long noticeDetailsId);

    List<Long> findUpcomingIds(NoticeReminderStatus status, LocalDateTime until);

    List<Long> findSchedulableIds(NoticeReminderStatus status, LocalDateTime now, LocalDateTime until);

    Optional<LocalDateTime> claimForScheduling(
            Long id,
            NoticeReminderStatus status,
            LocalDateTime now,
            Duration leaseGrace
    );

    long recoverStaleProcessing(
            NoticeReminderStatus processing,
            NoticeReminderStatus pending,
            LocalDateTime now
    );

    long releaseAfterFailure(
            Long id,
            int maxRetryCount,
            NoticeReminderStatus pending,
            NoticeReminderStatus failed
    );
}
