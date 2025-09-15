package com.dongsoop.dongsoop.report.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.exception.SanctionAlreadyExistsException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public void processSanction(Member admin, Member targetMember, Sanction sanction) {
        if (this.isProcessed) {
            throw new SanctionAlreadyExistsException(this.id);
        }

        this.admin = admin;
        this.targetMember = targetMember;
        this.sanction = sanction;
        this.isProcessed = true;
    }

    public void markAsProcessedWithoutSanction() {
        this.isProcessed = true;
    }
}
