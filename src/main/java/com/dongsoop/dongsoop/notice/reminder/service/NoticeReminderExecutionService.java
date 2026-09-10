package com.dongsoop.dongsoop.notice.reminder.service;

import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminderStatus;
import com.dongsoop.dongsoop.notice.reminder.repository.NoticeReminderRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.dto.NotificationSend;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class NoticeReminderExecutionService {

    private static final String TITLE = "공지 리마인더";
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration PROCESSING_LEASE = Duration.ofMinutes(10);
    private static final int MAX_RETRY_COUNT = 3;

    private final NoticeReminderRepository noticeReminderRepository;
    private final NotificationSendService notificationSendService;
    private final TransactionTemplate transactionTemplate;

    @Value("${university.domain}")
    private String universityDomain;

    public void execute(Long reminderId, LocalDateTime expectedRemindAt) {
        PreparedReminder prepared = prepare(reminderId, expectedRemindAt);
        if (prepared == null) {
            return;
        }

        try {
            notificationSendService.send(
                    List.of(prepared.deviceToken()),
                    new NotificationSend(
                            reminderId,
                            TITLE,
                            prepared.noticeTitle(),
                            NotificationType.NOTICE,
                            prepared.noticeLink()
                    )
            );
            markSent(reminderId, expectedRemindAt);
        } catch (Exception exception) {
            markDeliveryFailed(reminderId, expectedRemindAt);
            throw exception;
        }
    }

    private PreparedReminder prepare(Long reminderId, LocalDateTime expectedRemindAt) {
        return transactionTemplate.execute(status -> {
            NoticeReminder reminder = noticeReminderRepository.findByIdForUpdate(reminderId).orElse(null);
            if (reminder == null || reminder.getStatus() != NoticeReminderStatus.PENDING) {
                return null;
            }

            if (!reminder.getRemindAt().equals(expectedRemindAt)) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
            if (reminder.getRemindAt().isAfter(now)) {
                return null;
            }

            reminder.markProcessing(now.plus(PROCESSING_LEASE));

            return new PreparedReminder(
                    reminder.getDevice().getDeviceToken(),
                    reminder.getNoticeDetails().getTitle(),
                    universityDomain + reminder.getNoticeDetails().getLink()
            );
        });
    }

    private void markSent(Long reminderId, LocalDateTime expectedRemindAt) {
        transactionTemplate.executeWithoutResult(status -> {
            NoticeReminder reminder = noticeReminderRepository.findByIdForUpdate(reminderId).orElse(null);
            if (reminder == null
                    || reminder.getStatus() != NoticeReminderStatus.PROCESSING
                    || !reminder.getRemindAt().equals(expectedRemindAt)) {
                return;
            }

            reminder.markSent(LocalDateTime.now(SEOUL_ZONE));
        });
    }

    private void markDeliveryFailed(Long reminderId, LocalDateTime expectedRemindAt) {
        transactionTemplate.executeWithoutResult(status -> {
            NoticeReminder reminder = noticeReminderRepository.findByIdForUpdate(reminderId).orElse(null);
            if (reminder == null
                    || reminder.getStatus() != NoticeReminderStatus.PROCESSING
                    || !reminder.getRemindAt().equals(expectedRemindAt)) {
                return;
            }

            reminder.markDeliveryFailed(MAX_RETRY_COUNT);
        });
    }

    private record PreparedReminder(
            String deviceToken,
            String noticeTitle,
            String noticeLink
    ) {
    }
}
