package com.dongsoop.dongsoop.study.entity;

import com.dongsoop.dongsoop.board.RecruitmentBoard;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SuperBuilder
@NoArgsConstructor
@SequenceGenerator(name = "study_board_sequence_generator")
@SQLRestriction("is_deleted = false")
public class StudyBoard extends RecruitmentBoard {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "study_board_sequence_generator")
    private Long id;

    public boolean equalsId(StudyBoard that) {
        return Objects.equals(this.id, that.id);
    }
}
