package com.dongsoop.dongsoop.eclass.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SequenceGenerator(name = "eclass_assignment_sequence_generator")
@Table(name = "eclass_assignment",
        uniqueConstraints = @UniqueConstraint(name = "uk_eclass_assignment_link_assign",
                columnNames = {"eclass_link_id", "assign_id"}))
public class EclassAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "eclass_assignment_sequence_generator")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eclass_link_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EclassLink link;

    @Column(name = "assign_id", nullable = false)
    private Long assignId;

    @Column(name = "course_module_id", nullable = false)
    private Long courseModuleId;

    @Column(name = "course_name", length = 100, nullable = false)
    private String courseName;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;

    @Column(name = "cutoff_at")
    private LocalDateTime cutoffAt;

    @Column(name = "submitted", nullable = false)
    private boolean submitted = false;

    @Column(name = "submission_checked_at")
    private LocalDateTime submissionCheckedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "last_reminded_days")
    private Integer lastRemindedDays;

    public EclassAssignment(EclassLink link, Long assignId, Long courseModuleId, String courseName, String title,
                            LocalDateTime dueAt, LocalDateTime cutoffAt) {
        this.link = link;
        this.assignId = assignId;
        this.courseModuleId = courseModuleId;
        this.courseName = courseName;
        this.title = title;
        this.dueAt = dueAt;
        this.cutoffAt = cutoffAt;
    }

    /**
     * 마감이 바뀌면 리마인드 단계를 처음부터 다시 센다 — 연장된 마감은 새 일정으로 알려야 한다.
     * 삭제 표시된 과제가 다시 응답에 나타나면 복구한다.
     */
    public void update(String courseName, String title, LocalDateTime dueAt, LocalDateTime cutoffAt) {
        if (!Objects.equals(this.dueAt, dueAt)) {
            this.lastRemindedDays = null;
        }

        this.courseName = courseName;
        this.title = title;
        this.dueAt = dueAt;
        this.cutoffAt = cutoffAt;
        this.removedAt = null;
    }

    public void markSubmitted(LocalDateTime now) {
        this.submitted = true;
        this.submissionCheckedAt = now;
    }

    public void markChecked(LocalDateTime now) {
        this.submissionCheckedAt = now;
    }

    public void markRemoved(LocalDateTime now) {
        this.removedAt = now;
    }

    public void markReminded(int daysBefore) {
        this.lastRemindedDays = daysBefore;
    }

    public boolean isRemoved() {
        return this.removedAt != null;
    }

    public boolean needsReminder(int daysBefore) {
        return this.lastRemindedDays == null || daysBefore < this.lastRemindedDays;
    }
}
