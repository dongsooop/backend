package com.dongsoop.dongsoop.notice.reminder.service;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.exception.UnregisteredDeviceException;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.reminder.dto.NoticeReminderResponse;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.exception.InvalidNoticeReminderTimeException;
import com.dongsoop.dongsoop.notice.reminder.exception.NoticeDetailsNotFoundException;
import com.dongsoop.dongsoop.notice.reminder.exception.NoticeReminderNotFoundException;
import com.dongsoop.dongsoop.notice.reminder.repository.NoticeReminderRepository;
import com.dongsoop.dongsoop.notice.reminder.scheduler.NoticeReminderScheduler;
import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NoticeReminderService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final NoticeReminderRepository noticeReminderRepository;
    private final NoticeDetailsRepository noticeDetailsRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final NoticeReminderScheduler noticeReminderScheduler;

    @Transactional
    public NoticeReminderResponse upsert(Long noticeId, String fid, String deviceToken, LocalDateTime remindAt) {
        validateRemindAt(remindAt);

        MemberDevice device = resolveDevice(fid, deviceToken);
        NoticeDetails noticeDetails = noticeDetailsRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeDetailsNotFoundException(noticeId));

        NoticeReminder reminder = noticeReminderRepository
                .findByDeviceIdAndNoticeDetailsIdForUpdate(device.getId(), noticeId)
                .map(existing -> {
                    existing.reschedule(remindAt);
                    return existing;
                })
                .orElseGet(() -> new NoticeReminder(device, noticeDetails, remindAt));

        NoticeReminder saved = noticeReminderRepository.save(reminder);
        scheduleAfterCommit(saved.getId());

        return new NoticeReminderResponse(noticeId, remindAt);
    }

    @Transactional(readOnly = true)
    public NoticeReminderResponse get(Long noticeId, String fid, String deviceToken) {
        MemberDevice device = resolveDevice(fid, deviceToken);

        NoticeReminder reminder = noticeReminderRepository
                .findByDeviceIdAndNoticeDetailsId(device.getId(), noticeId)
                .orElseThrow(NoticeReminderNotFoundException::new);

        return new NoticeReminderResponse(noticeId, reminder.getRemindAt());
    }

    @Transactional
    public void delete(Long noticeId, String fid, String deviceToken) {
        MemberDevice device = resolveDevice(fid, deviceToken);

        NoticeReminder reminder = noticeReminderRepository
                .findByDeviceIdAndNoticeDetailsId(device.getId(), noticeId)
                .orElseThrow(NoticeReminderNotFoundException::new);

        noticeReminderRepository.delete(reminder);
    }

    private MemberDevice resolveDevice(String fid, String deviceToken) {
        if (!StringUtils.hasText(fid) || !StringUtils.hasText(deviceToken)) {
            throw new UnregisteredDeviceException();
        }

        MemberDevice device = memberDeviceRepository.findByFid(fid)
                .orElseThrow(UnregisteredDeviceException::new);

        if (!deviceToken.equals(device.getDeviceToken())) {
            throw new UnregisteredDeviceException();
        }

        return device;
    }

    private void validateRemindAt(LocalDateTime remindAt) {
        if (!remindAt.isAfter(LocalDateTime.now(SEOUL_ZONE))) {
            throw new InvalidNoticeReminderTimeException();
        }
    }

    private void scheduleAfterCommit(Long reminderId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                noticeReminderScheduler.scheduleIfUpcoming(reminderId);
            }
        });
    }
}
