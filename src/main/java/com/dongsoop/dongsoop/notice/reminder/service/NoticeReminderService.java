package com.dongsoop.dongsoop.notice.reminder.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import com.dongsoop.dongsoop.notice.reminder.dto.NoticeReminderResponse;
import com.dongsoop.dongsoop.notice.reminder.entity.NoticeReminder;
import com.dongsoop.dongsoop.notice.reminder.exception.NoticeDetailsNotFoundException;
import com.dongsoop.dongsoop.notice.reminder.exception.NoticeReminderNotFoundException;
import com.dongsoop.dongsoop.notice.reminder.repository.NoticeReminderRepository;
import com.dongsoop.dongsoop.notice.reminder.scheduler.NoticeReminderScheduler;
import com.dongsoop.dongsoop.notice.repository.NoticeDetailsRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class NoticeReminderService {

    private final NoticeReminderRepository noticeReminderRepository;
    private final NoticeDetailsRepository noticeDetailsRepository;
    private final MemberService memberService;
    private final NoticeReminderScheduler noticeReminderScheduler;

    @Transactional
    public NoticeReminderResponse upsert(Long noticeId, LocalDateTime remindAt) {
        Member member = memberService.getMemberReferenceByContext();
        NoticeDetails noticeDetails = noticeDetailsRepository.findById(noticeId)
                .orElseThrow(() -> new NoticeDetailsNotFoundException(noticeId));

        NoticeReminder reminder = noticeReminderRepository
                .findByMemberIdAndNoticeDetailsId(member.getId(), noticeId)
                .map(existing -> {
                    existing.reschedule(remindAt);
                    return existing;
                })
                .orElseGet(() -> new NoticeReminder(member, noticeDetails, remindAt));

        NoticeReminder saved = noticeReminderRepository.save(reminder);
        scheduleAfterCommit(saved.getId());

        return new NoticeReminderResponse(noticeId, remindAt);
    }

    @Transactional(readOnly = true)
    public NoticeReminderResponse get(Long noticeId) {
        Long memberId = memberService.getMemberIdByAuthentication();

        NoticeReminder reminder = noticeReminderRepository
                .findByMemberIdAndNoticeDetailsId(memberId, noticeId)
                .orElseThrow(NoticeReminderNotFoundException::new);

        return new NoticeReminderResponse(noticeId, reminder.getRemindAt());
    }

    @Transactional
    public void delete(Long noticeId) {
        Long memberId = memberService.getMemberIdByAuthentication();

        NoticeReminder reminder = noticeReminderRepository
                .findByMemberIdAndNoticeDetailsId(memberId, noticeId)
                .orElseThrow(NoticeReminderNotFoundException::new);

        noticeReminderRepository.delete(reminder);
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
