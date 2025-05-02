package com.dongsoop.dongsoop.board;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class RecruitmentBoard extends Board {

    @NotNull
    @Column(name = "recruitment_deadline", nullable = false)
    private LocalDateTime recruitmentDeadline;

    @NotNull
    @Column(name = "recruitment_capacity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer recruitmentCapacity;

    @Column(name = "tags", length = 100)
    private String tags;
}
