package com.dongsoop.dongsoop.timetable.dto;

import com.dongsoop.dongsoop.date.EndAtAfterStartAt;
import com.dongsoop.dongsoop.timetable.entity.SemesterType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Year;
import org.hibernate.validator.constraints.Length;

@EndAtAfterStartAt
public record CreateTimetableRequest(

        @NotBlank(message = "이름은 필수 입력값입니다.")
        @Length(max = 15, message = "이름은 최대 15자까지 입력할 수 있습니다.")
        String name,

        @Length(max = 8, message = "교수 이름은 최대 8자까지 입력할 수 있습니다.")
        String professor,

        @Length(max = 10, message = "위치는 최대 10자까지 입력할 수 있습니다.")
        String location,

        @NotNull(message = "요일은 필수 입력값입니다.")
        DayOfWeek week,

        @NotNull(message = "시작 시간은 필수 입력값입니다.")
        LocalTime startAt,

        @NotNull(message = "종료 시간은 필수 입력값입니다.")
        LocalTime endAt,

        @NotNull(message = "학기 연도는 필수 입력값입니다.")
        Year year,

        @NotNull(message = "학기 유형은 필수 입력값입니다.")
        SemesterType semester
) {
}
