package com.dongsoop.dongsoop.tutoring.entity;

import com.dongsoop.dongsoop.board.RecruitmentBoard;
import com.dongsoop.dongsoop.department.entity.Department;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.validation.constraints.NotNull;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
@SequenceGenerator(name = "tutoring_board_sequence_generator")
public class TutoringBoard extends RecruitmentBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tutoring_board_sequence_generator")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;
}
