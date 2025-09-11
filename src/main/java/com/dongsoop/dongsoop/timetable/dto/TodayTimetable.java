package com.dongsoop.dongsoop.timetable.dto;

import java.time.LocalTime;

public record TodayTimetable(

        String name,
        LocalTime startAt,
        LocalTime endAt,
        Long memberId
) {
}
