package com.dongsoop.dongsoop.timetable.dto;

import java.time.LocalTime;

public record OverlapTimetable(
        LocalTime startAt,
        LocalTime endAt
) {
}
