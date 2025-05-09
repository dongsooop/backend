package com.dongsoop.dongsoop.tutoring.dto;

import com.dongsoop.dongsoop.date.EndAtAfterStartAt;
import com.dongsoop.dongsoop.date.MaxDate;
import com.dongsoop.dongsoop.date.MaxDuration;
import com.dongsoop.dongsoop.date.MinDuration;
import com.dongsoop.dongsoop.date.TodayOrFuture;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@MaxDuration // 최대 4주 (28일)
@MinDuration // 최소 하루
@EndAtAfterStartAt
public class CreateTutoringBoardRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotBlank
    private String tags;

    @NotNull
    @Min(1)
    private Integer capacity;

    @NotNull
    @MaxDate(month = 3)
    @TodayOrFuture
    private LocalDateTime startAt;

    @NotNull
    @TodayOrFuture
    private LocalDateTime endAt;

    @NotNull
    private DepartmentType departmentType;
}
