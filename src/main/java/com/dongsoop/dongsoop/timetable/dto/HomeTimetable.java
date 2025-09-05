package com.dongsoop.dongsoop.timetable.dto;

import java.time.LocalTime;

public record HomeTimetable(

        String title,
        LocalTime startAt,
        LocalTime endAt
) {
}
