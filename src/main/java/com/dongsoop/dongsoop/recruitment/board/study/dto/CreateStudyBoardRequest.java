package com.dongsoop.dongsoop.recruitment.board.study.dto;

import com.dongsoop.dongsoop.date.EndAtAfterStartAt;
import com.dongsoop.dongsoop.date.MaxDate;
import com.dongsoop.dongsoop.date.MaxDuration;
import com.dongsoop.dongsoop.date.MinDuration;
import com.dongsoop.dongsoop.date.TodayOrFuture;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.recruitment.validation.annotation.BoardTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@MaxDuration // 최대 4주 (28일)
@MinDuration // 최소 하루
@EndAtAfterStartAt
public record CreateStudyBoardRequest(

        @NotBlank
        String title,

        @NotBlank
        String content,

        @BoardTag
        String tags,

        @NotNull
        @MaxDate(month = 3)
        @TodayOrFuture
        LocalDateTime startAt,

        @NotNull
        @TodayOrFuture
        LocalDateTime endAt,

        @NotNull
        @Size(min = 1)
        List<DepartmentType> departmentTypeList
) {
}
