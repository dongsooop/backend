package com.dongsoop.dongsoop.board;

import com.dongsoop.dongsoop.recruitment.validation.constant.RecruitmentValidationConstant;
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

    @Column(name = "tags", length = RecruitmentValidationConstant.TAG_MAX_LENGTH, nullable = false)
    private String tags;
}
