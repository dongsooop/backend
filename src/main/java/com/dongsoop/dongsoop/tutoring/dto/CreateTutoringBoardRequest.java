package com.dongsoop.dongsoop.tutoring.dto;

import com.dongsoop.dongsoop.date.TodayOrFuture;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTutoringBoardRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotBlank
    String tags;

    @NotNull
    @Min(1)
    Integer recruitmentCapacity;

    @NotNull
    @TodayOrFuture
    LocalDateTime startAt;

    @NotNull
    @TodayOrFuture
    LocalDateTime endAt;

    @NotNull
    DepartmentType departmentType;
}
