package com.dongsoop.dongsoop.recruitment.study.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.recruitment.entity.ApplyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@SuperBuilder
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class StudyApply {

    @EmbeddedId
    private StudyApplyKey id;

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
    private ApplyStatus status = ApplyStatus.APPLY;

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudyApplyKey {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "study_board_id", updatable = false)
        private StudyBoard studyBoard;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "member_id", updatable = false)
        private Member member;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StudyApplyKey that = (StudyApplyKey) o;
            return this.studyBoard.equalsId(that.studyBoard)
                    && Objects.equals(this.member.getId(), that.member.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(studyBoard.getId(), member.getId());
        }
    }
}
