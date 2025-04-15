package com.dongsoop.dongsoop.tutoring.entity;

import com.dongsoop.dongsoop.board.RecruitmentBoard;
import com.dongsoop.dongsoop.department.entity.Department;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

@Entity
public class TutoringBoard extends RecruitmentBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @JoinColumn(name = "recruitment_department_id", nullable = false)
    private Department recruitmentDepartment;
}
