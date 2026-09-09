package com.dongsoop.dongsoop.notice.reminder.scheduler;

import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminderStatus;
import com.dongsoop.dongsoop.notice.reminder.repository.NoticeReminderRepository;
import com.dongsoop.dongsoop.notice.reminder.service.NoticeReminderExecutionService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoticeReminderScheduler {

    private static final Duration LOOK_AHEAD = Duration.ofMinutes(65);
    private static final Duration LEASE_GRACE = Duration.ofMinutes(10);
    private static final int MAX_RETRY_COUNT = 3;
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final NoticeReminderRepository noticeReminderRepository;
    private final NoticeReminderExecutionService executionService;
    private final TaskScheduler taskScheduler;

    public NoticeReminderScheduler(
            NoticeReminderRepository noticeReminderRepository,
            NoticeReminderExecutionService executionService,
            @Qualifier("noticeReminderTaskScheduler") TaskScheduler taskScheduler
    ) {
        this.noticeReminderRepository = noticeReminderRepository;
        this.executionService = executionService;
        this.taskScheduler = taskScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleOnStartup() {
        scheduleUpcoming();
    }

    @Scheduled(fixedRate = 60 * 60 * 1000L)
    public void scheduleUpcoming() {
        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        LocalDateTime until = now.plus(LOOK_AHEAD);

        List<Long> reminderIds = noticeReminderRepository.findSchedulableIds(
                NoticeReminderStatus.PENDING,
                now,
                until
        );

        reminderIds.forEach(this::claimAndSchedule);
    }

    public void scheduleIfUpcoming(Long reminderId) {
        NoticeReminder reminder = noticeReminderRepository.findById(reminderId).orElse(null);
        if (reminder == null || reminder.getStatus() != NoticeReminderStatus.PENDING) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        if (!reminder.getRemindAt().isAfter(now.plus(LOOK_AHEAD))) {
            claimAndSchedule(reminderId);
        }
    }

    private void claimAndSchedule(Long reminderId) {
        NoticeReminder reminder = noticeReminderRepository.findById(reminderId).orElse(null);
        if (reminder == null || reminder.getStatus() != NoticeReminderStatus.PENDING) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        LocalDateTime claimedUntil = reminder.getRemindAt().isAfter(now)
                ? reminder.getRemindAt().plus(LEASE_GRACE)
                : now.plus(LEASE_GRACE);

        int claimed = noticeReminderRepository.claim(
                reminderId,
                NoticeReminderStatus.PENDING,
                now,
                claimedUntil
        );
        if (claimed == 0) {
            return;
        }

        LocalDateTime expectedRemindAt = reminder.getRemindAt();
        LocalDateTime executeAt = expectedRemindAt.isBefore(now) ? now : expectedRemindAt;

        try {
            taskScheduler.schedule(
                    () -> execute(reminderId, expectedRemindAt),
                    executeAt.atZone(SEOUL_ZONE).toInstant()
            );
        } catch (RuntimeException exception) {
            noticeReminderRepository.releaseAfterFailure(
                    reminderId,
                    MAX_RETRY_COUNT,
                    NoticeReminderStatus.PENDING,
                    NoticeReminderStatus.FAILED
            );
            throw exception;
        }
    }

    private void execute(Long reminderId, LocalDateTime expectedRemindAt) {
        try {
            executionService.execute(reminderId, expectedRemindAt);
        } catch (Exception exception) {
            noticeReminderRepository.releaseAfterFailure(
                    reminderId,
                    MAX_RETRY_COUNT,
                    NoticeReminderStatus.PENDING,
                    NoticeReminderStatus.FAILED
            );
            log.error("Failed to execute notice reminder. reminderId={}", reminderId, exception);
        }
    }
}
