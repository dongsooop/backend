package com.dongsoop.dongsoop.notice.reminder.entity;

import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.notice.entity.NoticeDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notice_reminder",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notice_reminder_device_notice",
                columnNames = {"device_id", "notice_details_id"}
        )
)
public class NoticeReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private MemberDevice device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_details_id", nullable = false)
    private NoticeDetails noticeDetails;

    @Column(name = "remind_at", nullable = false)
    private LocalDateTime remindAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NoticeReminderStatus status;

    @Column(name = "claimed_until")
    private LocalDateTime claimedUntil;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    public NoticeReminder(MemberDevice device, NoticeDetails noticeDetails, LocalDateTime remindAt) {
        this.device = device;
        this.noticeDetails = noticeDetails;
        this.remindAt = remindAt;
        this.status = NoticeReminderStatus.PENDING;
        this.retryCount = 0;
    }

    public void claimUntil(LocalDateTime claimedUntil) {
        this.claimedUntil = claimedUntil;
    }

    public void reschedule(LocalDateTime remindAt) {
        this.remindAt = remindAt;
        this.status = NoticeReminderStatus.PENDING;
        this.claimedUntil = null;
        this.sentAt = null;
        this.retryCount = 0;
    }

    public void markProcessing(LocalDateTime processingUntil) {
        this.status = NoticeReminderStatus.PROCESSING;
        this.claimedUntil = processingUntil;
    }

    public void markSent(LocalDateTime sentAt) {
        this.status = NoticeReminderStatus.SENT;
        this.sentAt = sentAt;
        this.claimedUntil = null;
    }

    public void markDeliveryFailed(int maxRetryCount) {
        this.claimedUntil = null;
        this.retryCount++;

        if (this.retryCount >= maxRetryCount) {
            this.status = NoticeReminderStatus.FAILED;
            return;
        }

        this.status = NoticeReminderStatus.PENDING;
    }

    public void recoverProcessing() {
        this.status = NoticeReminderStatus.PENDING;
        this.claimedUntil = null;
    }

    public void releaseClaimAfterFailure() {
        this.claimedUntil = null;
        this.retryCount++;
    }

    public void markFailed() {
        this.status = NoticeReminderStatus.FAILED;
        this.claimedUntil = null;
        this.retryCount++;
    }
}
