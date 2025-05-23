package com.dongsoop.dongsoop.study.entity;

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

@Entity
@SuperBuilder
@NoArgsConstructor
public class StudyBoardApplication {

    @EmbeddedId
    private StudyBoardApplicationKey id;

    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudyBoardApplicationKey {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "study_board_id", updatable = false)
        private StudyBoard studyBoard;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(nullable = false, name = "member_id", updatable = false)
        private Member member;
    }
}
