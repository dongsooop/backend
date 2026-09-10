package com.dongsoop.dongsoop.notice.reminder.repository;

import com.dongsoop.dongsoop.memberdevice.entity.QMemberDevice;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminderStatus;
import com.dongsoop.dongsoop.notice.reminder.entity.QNoticeReminder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import java.time.Duration;
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
    private static final QMemberDevice memberDevice = QMemberDevice.memberDevice;

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
    public void lockDevice(Long deviceId) {
        queryFactory
                .select(memberDevice.id)
                .from(memberDevice)
                .where(memberDevice.id.eq(deviceId))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchOne();
    }

    @Override
    public Optional<NoticeReminder> findByDeviceIdAndNoticeDetailsIdForUpdate(Long deviceId, Long noticeDetailsId) {
        NoticeReminder result = queryFactory
                .selectFrom(noticeReminder)
                .where(
                        noticeReminder.device.id.eq(deviceId),
                        noticeReminder.noticeDetails.id.eq(noticeDetailsId)
                )
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
    public Optional<LocalDateTime> claimForScheduling(
            Long id,
            NoticeReminderStatus status,
            LocalDateTime now,
            Duration leaseGrace
    ) {
        NoticeReminder reminder = findByIdForUpdate(id).orElse(null);
        if (reminder == null || reminder.getStatus() != status) {
            return Optional.empty();
        }

        LocalDateTime claimedUntil = reminder.getClaimedUntil();
        if (claimedUntil != null && !claimedUntil.isBefore(now)) {
            return Optional.empty();
        }

        LocalDateTime remindAt = reminder.getRemindAt();
        reminder.claimUntil(resolveLeaseUntil(remindAt, now, leaseGrace));
        return Optional.of(remindAt);
    }

    private LocalDateTime resolveLeaseUntil(
            LocalDateTime remindAt,
            LocalDateTime now,
            Duration leaseGrace
    ) {
        if (remindAt.isAfter(now)) {
            return remindAt.plus(leaseGrace);
        }

        return now.plus(leaseGrace);
    }

    @Override
    @Transactional
    public long recoverStaleProcessing(
            NoticeReminderStatus processing,
            NoticeReminderStatus pending,
            LocalDateTime now
    ) {
        return queryFactory
                .update(noticeReminder)
                .set(noticeReminder.status, pending)
                .setNull(noticeReminder.claimedUntil)
                .where(
                        noticeReminder.status.eq(processing),
                        noticeReminder.claimedUntil.isNotNull(),
                        noticeReminder.claimedUntil.lt(now)
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
