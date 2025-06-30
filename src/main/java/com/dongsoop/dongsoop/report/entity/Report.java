package com.dongsoop.dongsoop.report.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    @Column(nullable = false)
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

    @Enumerated(EnumType.STRING)
    private SanctionType sanctionType;

    @Column(length = 500)
    private String sanctionReason;

    private LocalDateTime sanctionStartAt;
    private LocalDateTime sanctionEndAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isProcessed = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isSanctionActive = false;

    public void processSanction(Member admin, Member targetMember, SanctionType sanctionType,
                                String sanctionReason, LocalDateTime sanctionEndAt) {
        this.admin = admin;
        this.targetMember = targetMember;
        this.sanctionType = sanctionType;
        this.sanctionReason = sanctionReason;
        this.sanctionStartAt = LocalDateTime.now();
        this.sanctionEndAt = sanctionEndAt;
        this.isProcessed = true;
        this.isSanctionActive = true;
    }
}
