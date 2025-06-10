package com.dongsoop.dongsoop.recruitment.study.entity;

import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
public class StudyApply {

    @EmbeddedId
    private StudyApplyKey id;

    @Column(name = "introduction", length = 500)
    private String introduction;

    @Column(name = "motivation", length = 500)
    private String motivation;

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
