package com.dongsoop.dongsoop.notice.reminder.repository;

import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminderStatus;
import com.dongsoop.dongsoop.notice.reminder.entity.QNoticeReminder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class NoticeReminderRepositoryCustomImpl implements NoticeReminderRepositoryCustom {

    private static final QNoticeReminder noticeReminder = QNoticeReminder.noticeReminder;

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<NoticeReminder> findByIdForUpdate(Long id) {
        NoticeReminder result = queryFactory
                .selectFrom(noticeReminder)
                .where(noticeReminder.id.eq(id))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<Long> findUpcomingIds(NoticeReminderStatus status, LocalDateTime until) {
        return queryFactory
                .select(noticeReminder.id)
                .from(noticeReminder)
                .where(
                        noticeReminder.status.eq(status),
                        noticeReminder.remindAt.loe(until)
                )
                .orderBy(noticeReminder.remindAt.asc())
                .fetch();
    }

    @Override
    public List<Long> findSchedulableIds(
            NoticeReminderStatus status,
            LocalDateTime now,
            LocalDateTime until
    ) {
        return queryFactory
                .select(noticeReminder.id)
                .from(noticeReminder)
                .where(
                        noticeReminder.status.eq(status),
                        noticeReminder.remindAt.loe(until),
                        noticeReminder.claimedUntil.isNull()
                                .or(noticeReminder.claimedUntil.lt(now))
                )
                .orderBy(noticeReminder.remindAt.asc())
                .fetch();
    }

    @Override
    @Transactional
    public long claim(
            Long id,
            NoticeReminderStatus status,
            LocalDateTime now,
            LocalDateTime claimedUntil
    ) {
        return queryFactory
                .update(noticeReminder)
                .set(noticeReminder.claimedUntil, claimedUntil)
                .where(
                        noticeReminder.id.eq(id),
                        noticeReminder.status.eq(status),
                        noticeReminder.claimedUntil.isNull()
                                .or(noticeReminder.claimedUntil.lt(now))
                )
                .execute();
    }

    @Override
    @Transactional
    public long releaseAfterFailure(
            Long id,
            int maxRetryCount,
            NoticeReminderStatus pending,
            NoticeReminderStatus failed
    ) {
        NoticeReminder reminder = findByIdForUpdate(id).orElse(null);
        if (reminder == null || reminder.getStatus() != pending) {
            return 0;
        }

        if (reminder.getRetryCount() + 1 >= maxRetryCount) {
            reminder.markFailed();
            return 1;
        }

        reminder.releaseClaimAfterFailure();
        return 1;
    }
}
