package com.dongsoop.dongsoop.recruitment.tutoring.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@SuperBuilder
@NoArgsConstructor
@SQLRestriction("is_deleted = false")
@EntityListeners(AuditingEntityListener.class)
public class TutoringApply extends BaseEntity {

    @EmbeddedId
    private TutoringApplyKey id;

    @Column(name = "introduction", length = 500)
    private String introduction;

    @Column(name = "motivation", length = 500)
    private String motivation;

    @Column(name = "apply_time", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime applyTime;

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TutoringApplyKey {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "tutoring_board_id", updatable = false)
        private TutoringBoard tutoringBoard;

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

            TutoringApplyKey that = (TutoringApplyKey) o;
            return this.tutoringBoard.equalsId(that.tutoringBoard)
                    && Objects.equals(this.member.getId(), that.member.getId());
        }

        @Override
        public int hashCode() {
            return Objects.hash(tutoringBoard.getId(), member.getId());
        }
    }
}
