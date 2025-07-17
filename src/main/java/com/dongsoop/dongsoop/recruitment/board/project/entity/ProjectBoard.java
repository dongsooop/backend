package com.dongsoop.dongsoop.recruitment.board.project.entity;

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
@SequenceGenerator(name = "project_board_sequence_generator")
@SQLRestriction("is_deleted = false")
public class ProjectBoard extends RecruitmentBoard {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "project_board_sequence_generator")
    private Long id;

    public boolean equalsId(ProjectBoard that) {
        return Objects.equals(this.id, that.id);
    }
}
