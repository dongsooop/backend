package com.dongsoop.dongsoop.recruitment.apply.tutoring.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.board.tutoring.entity.TutoringBoard;
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
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@IdClass(TutoringApply.TutoringApplyKey.class)
@SuperBuilder
@NoArgsConstructor
@SQLRestriction("is_deleted = false")
@EntityListeners(AuditingEntityListener.class)
public class TutoringApply extends BaseEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name = "tutoring_board_id", updatable = false)
    private TutoringBoard tutoringBoard;

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

    public TutoringBoard getTutoringBoard() {
        return tutoringBoard;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class TutoringApplyKey implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long tutoringBoard;

        private Long member;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TutoringApplyKey that = (TutoringApplyKey) o;
            return Objects.equals(tutoringBoard, that.tutoringBoard)
                    && Objects.equals(member, that.member);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tutoringBoard, member);
        }
    }
}
