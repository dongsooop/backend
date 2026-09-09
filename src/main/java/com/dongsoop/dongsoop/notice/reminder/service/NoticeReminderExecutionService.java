package com.dongsoop.dongsoop.notice.reminder.service;

import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminderStatus;
import com.dongsoop.dongsoop.notice.reminder.repository.NoticeReminderRepository;
import com.dongsoop.dongsoop.notification.constant.NotificationType;
import com.dongsoop.dongsoop.notification.entity.MemberNotification;
import com.dongsoop.dongsoop.notification.service.NotificationSaveService;
import com.dongsoop.dongsoop.notification.service.NotificationSendService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeReminderExecutionService {

    private static final String TITLE = "공지 리마인더";
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final NoticeReminderRepository noticeReminderRepository;
    private final NotificationSaveService notificationSaveService;
    private final NotificationSendService notificationSendService;

    @Value("${university.domain}")
    private String universityDomain;

    @Transactional
    public void execute(Long reminderId, LocalDateTime expectedRemindAt) {
        NoticeReminder reminder = noticeReminderRepository.findByIdForUpdate(reminderId).orElse(null);
        if (reminder == null || reminder.getStatus() != NoticeReminderStatus.PENDING) {
            return;
        }

        if (!reminder.getRemindAt().equals(expectedRemindAt)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(SEOUL_ZONE);
        if (reminder.getRemindAt().isAfter(now)) {
            return;
        }

        String noticeLink = universityDomain + reminder.getNoticeDetails().getLink();
        MemberNotification notification = notificationSaveService.save(
                reminder.getMember(),
                TITLE,
                reminder.getNoticeDetails().getTitle(),
                NotificationType.NOTICE,
                noticeLink
        );

        notificationSendService.send(notification);
        reminder.markSent(now);
    }
}
