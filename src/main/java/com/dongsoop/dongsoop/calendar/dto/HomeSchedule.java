package com.dongsoop.dongsoop.calendar.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record HomeSchedule(

        String title,
        LocalTime startAt,
        LocalTime endAt,
        ScheduleType type
) {
    public HomeSchedule(String title, LocalDateTime startAt, LocalDateTime endAt, ScheduleType type) {
        this(title, startAt.toLocalTime(), endAt.toLocalTime(), type);
    }

    public HomeSchedule(String title, LocalDate startAt, LocalDate endAt, ScheduleType type) {
        this(title, LocalTime.MIN, LocalTime.MAX, type);
    }
}
