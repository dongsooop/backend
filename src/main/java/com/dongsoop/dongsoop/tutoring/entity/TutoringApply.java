package com.dongsoop.dongsoop.tutoring.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SuperBuilder
@NoArgsConstructor
@SQLRestriction("is_deleted = false")
public class TutoringApply extends BaseEntity {

    @EmbeddedId
    private TutoringApplyKey id;

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
    }
}
