package com.dongsoop.dongsoop.board;

import com.dongsoop.dongsoop.tutoring.entity.BoardRecruitmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class RecruitmentBoard extends Board {

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(name = "recruitment_status", nullable = false)
    private BoardRecruitmentStatus recruitmentStatus;

    @NotNull
    @Column(name = "recruitment_deadline", nullable = false)
    private LocalDateTime recruitmentDeadline;

    @NotNull
    @Column(name = "recruitment_capacity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer recruitmentCapacity;

    @Column(name = "tags", length = 100)
    private String tags;
}
