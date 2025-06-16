package com.dongsoop.dongsoop.timetable.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record TimetableView(

        Long id,
        String name,
        String professor,
        String location,
        LocalTime startAt,
        LocalTime endAt,
        DayOfWeek week
) {
}
