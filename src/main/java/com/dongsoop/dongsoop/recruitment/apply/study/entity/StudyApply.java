package com.dongsoop.dongsoop.recruitment.apply.study.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoard;
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
@IdClass(StudyApply.StudyApplyKey.class)
@SuperBuilder
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StudyApply {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "study_board_id", updatable = false)
    private StudyBoard studyBoard;

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

    public StudyBoard getStudyBoard() {
        return studyBoard;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudyApplyKey implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long studyBoard;

        private Long member;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StudyApplyKey that = (StudyApplyKey) o;
            return Objects.equals(studyBoard, that.studyBoard)
                    && Objects.equals(member, that.member);
        }

        @Override
        public int hashCode() {
            return Objects.hash(studyBoard, member);
        }
    }
}
