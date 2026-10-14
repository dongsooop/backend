package com.dongsoop.dongsoop.recruitment.apply.project.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.board.project.entity.ProjectBoard;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@IdClass(ProjectApply.ProjectApplyKey.class)
@SuperBuilder
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ProjectApply {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "project_board_id", updatable = false)
    private ProjectBoard projectBoard;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "member_id", updatable = false)
    private Member member;

    @Column(name = "introduction", length = 500)
    private String introduction;

    @Column(name = "motivation", length = 500)
    private String motivation;

    @Column(name = "apply_time", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime applyTime;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RecruitmentApplyStatus status = RecruitmentApplyStatus.APPLY;

    public void updateStatus(RecruitmentApplyStatus status) {
        this.status = status;
    }

    public ProjectBoard getProjectBoard() {
        return projectBoard;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectApplyKey implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long projectBoard;

        private Long member;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ProjectApplyKey that = (ProjectApplyKey) o;
            return Objects.equals(projectBoard, that.projectBoard)
                    && Objects.equals(member, that.member);
        }

        @Override
        public int hashCode() {
            return Objects.hash(projectBoard, member);
        }
    }
}
