package com.dongsoop.dongsoop.board;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@SuperBuilder
@NoArgsConstructor
public abstract class RecruitmentBoard extends Board {

    @NotNull
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @NotNull
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @NotNull
    @Column(name = "recruitment_capacity", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer recruitmentCapacity;

    @Column(name = "tags", length = 100)
    private String tags;
}
