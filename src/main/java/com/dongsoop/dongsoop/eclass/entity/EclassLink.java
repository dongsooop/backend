package com.dongsoop.dongsoop.eclass.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 기기 1개당 이클래스 연동 1건. 회원/비회원 구분 없이 기기 단위로 관리한다 —
 * 학과 구독(device_notice_preference)과 같은 기준이라 비회원도 그대로 쓸 수 있다.
 *
 * <p>토큰은 AES-GCM 암호문으로만 저장한다. 비밀번호는 저장하지 않는다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(name = "eclass_link_sequence_generator")
@Table(name = "eclass_link")
public class EclassLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eclass_link_sequence_generator")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_device_id", nullable = false, unique = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private MemberDevice device;

    @Column(name = "moodle_user_id", nullable = false)
    private Long moodleUserId;

    @Column(name = "moodle_fullname", length = 50)
    private String moodleFullname;

    @Column(name = "token_encrypted", nullable = false, length = 512)
    private String tokenEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EclassLinkStatus status;

    @Column(name = "linked_at", nullable = false)
    private LocalDateTime linkedAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "token_valid_until")
    private LocalDateTime tokenValidUntil;

    @Column(name = "relink_requested_at")
    private LocalDateTime relinkRequestedAt;

    @Column(name = "expired_notified_at")
    private LocalDateTime expiredNotifiedAt;

    @Column(name = "last_manual_sync_at")
    private LocalDateTime lastManualSyncAt;

    public EclassLink(MemberDevice device, Long moodleUserId, String moodleFullname, String tokenEncrypted,
                      LocalDateTime now) {
        this.device = device;
        relink(moodleUserId, moodleFullname, tokenEncrypted, now);
    }

    public void relink(Long moodleUserId, String moodleFullname, String tokenEncrypted, LocalDateTime now) {
        this.moodleUserId = moodleUserId;
        this.moodleFullname = moodleFullname;
        this.tokenEncrypted = tokenEncrypted;
        this.status = EclassLinkStatus.ACTIVE;
        this.linkedAt = now;
        this.relinkRequestedAt = null;
        this.expiredNotifiedAt = null;
    }

    public void expire(LocalDateTime now) {
        this.status = EclassLinkStatus.EXPIRED;
        if (this.relinkRequestedAt == null) {
            this.relinkRequestedAt = now;
        }
    }

    public void markRelinkRequested(LocalDateTime now) {
        this.relinkRequestedAt = now;
    }

    public void markSynced(LocalDateTime now) {
        this.lastSyncedAt = now;
    }

    public void markExpiredNotified(LocalDateTime now) {
        this.expiredNotifiedAt = now;
    }

    public void markManualSync(LocalDateTime now) {
        this.lastManualSyncAt = now;
    }

    public boolean isActive() {
        return this.status == EclassLinkStatus.ACTIVE;
    }

    /**
     * 사일런트 재발급 지시를 보낸 지 timeout이 지나도록 재연동이 없으면, 보이는 알림으로 승격할 때가 된 것이다.
     */
    public boolean isRelinkOverdue(LocalDateTime now, Duration timeout) {
        return this.status == EclassLinkStatus.EXPIRED
                && this.relinkRequestedAt != null
                && this.expiredNotifiedAt == null
                && this.relinkRequestedAt.plus(timeout).isBefore(now);
    }

    public boolean isManualSyncOnCooldown(LocalDateTime now, Duration cooldown) {
        return this.lastManualSyncAt != null && this.lastManualSyncAt.plus(cooldown).isAfter(now);
    }

    public boolean needsPreemptiveRelink(LocalDateTime now, int noticeDays) {
        return this.status == EclassLinkStatus.ACTIVE
                && this.tokenValidUntil != null
                && this.relinkRequestedAt == null
                && this.tokenValidUntil.minusDays(noticeDays).isBefore(now);
    }
}
