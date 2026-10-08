package com.dongsoop.dongsoop.report.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.dto.MessageReportDraft;
import com.dongsoop.dongsoop.report.exception.ReportAlreadyProcessedException;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private Member reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reportReason", nullable = false)
    private ReportReason reportReason;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String targetUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Member admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_member_id")
    private Member targetMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sanction_id")
    private Sanction sanction;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isProcessed = false;

    @Column(name = "is_sanction_active", nullable = false)
    @Builder.Default
    private Boolean isSanctionActive = false;

    @Column(name = "chat_room_id", length = 64)
    private String chatRoomId;

    @Column(name = "message_id", length = 64)
    private String messageId;

    @Column(name = "message_content", length = 1000)
    private String messageContent;

    @Column(name = "message_sent_at")
    private LocalDateTime messageSentAt;

    @Convert(converter = ChatMessageSnapshotsConverter.class)
    @Column(name = "message_context", columnDefinition = "text")
    private ChatMessageSnapshots messageContext;

    @Convert(converter = ChatMessageSnapshotsConverter.class)
    @Column(name = "message_context_after", columnDefinition = "text")
    private ChatMessageSnapshots messageContextAfter;

    // 기존 행이 있는 운영 테이블에 ddl-auto가 NOT NULL 컬럼을 추가하려면 기본값이 필요하다
    @Column(name = "is_auto_reviewed", nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private Boolean isAutoReviewed = false;

    public static Report messageReport(Member reporter, MessageReportDraft draft, ReportReason reason,
                                       String description) {
        return Report.builder()
                .reporter(reporter)
                .reportType(draft.reportType())
                .targetId(draft.targetMember().getId())
                .targetMember(draft.targetMember())
                .reportReason(reason)
                .description(description)
                .targetUrl(draft.targetUrl())
                .chatRoomId(draft.chatRoomId())
                .messageId(draft.messageId())
                .messageContent(draft.messageContent())
                .messageSentAt(draft.messageSentAt())
                .messageContext(draft.messageContext())
                .build();
    }

    public void ensureNotProcessed() {
        if (this.isProcessed) {
            throw new ReportAlreadyProcessedException(this.id);
        }
    }

    public void processSanction(Member admin, Member targetMember, Sanction sanction) {
        ensureNotProcessed();

        this.admin = admin;
        this.targetMember = targetMember;
        this.sanction = sanction;
        this.isProcessed = true;
    }

    public void markAsProcessedWithoutSanction() {
        this.isProcessed = true;
    }

    public void dismiss(Member admin) {
        ensureNotProcessed();

        this.admin = admin;
        this.isProcessed = true;
    }

    public void recordContextAfter(ChatMessageSnapshots contextAfter) {
        if (this.messageContextAfter == null) {
            this.messageContextAfter = contextAfter;
        }
    }

    public void markAutoReviewed() {
        this.isAutoReviewed = true;
    }
}
