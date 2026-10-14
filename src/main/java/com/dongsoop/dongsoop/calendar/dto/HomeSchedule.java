package com.dongsoop.dongsoop.calendar.dto;

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

    public static HomeSchedule allDay(String title, ScheduleType type) {
        return new HomeSchedule(title, LocalTime.MIN, LocalTime.MAX, type);
    }
}
