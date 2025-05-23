package com.dongsoop.dongsoop.study.dto;

import com.dongsoop.dongsoop.date.EndAtAfterStartAt;
import com.dongsoop.dongsoop.date.MaxDate;
import com.dongsoop.dongsoop.date.MaxDuration;
import com.dongsoop.dongsoop.date.MinDuration;
import com.dongsoop.dongsoop.date.TodayOrFuture;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@MaxDuration // 최대 4주 (28일)
@MinDuration // 최소 하루
@EndAtAfterStartAt
public class CreateStudyBoardRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotBlank
    private String tags;

    @NotNull
    @MaxDate(month = 3)
    @TodayOrFuture
    private LocalDateTime startAt;

    @NotNull
    @TodayOrFuture
    private LocalDateTime endAt;

    private List<DepartmentType> departmentTypeList;
}
