package com.dongsoop.dongsoop.tutoring.dto;

import com.dongsoop.dongsoop.date.EndAtAfterStartAt;
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
    private Integer recruitmentCapacity;

    @NotNull
    @TodayOrFuture
    private LocalDateTime startAt;

    @NotNull
    @TodayOrFuture
    private LocalDateTime endAt;

    @NotNull
    private DepartmentType departmentType;

    public void validateEndAt() {
        if (startAt.isAfter(endAt)) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다.");
        }
    }
}
