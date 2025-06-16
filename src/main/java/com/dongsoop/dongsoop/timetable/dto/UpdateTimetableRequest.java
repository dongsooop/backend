package com.dongsoop.dongsoop.timetable.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.hibernate.validator.constraints.Length;

public record UpdateTimetableRequest(

        @NotNull
        Long id,

        @Length(max = 15, message = "이름은 최대 15자까지 입력할 수 있습니다.")
        String name,

        @Length(max = 8, message = "교수 이름은 최대 8자까지 입력할 수 있습니다.")
        String professor,

        @Length(max = 10, message = "위치는 최대 10자까지 입력할 수 있습니다.")
        String location,

        DayOfWeek week,
        LocalTime startAt,
        LocalTime endAt
) {
}
