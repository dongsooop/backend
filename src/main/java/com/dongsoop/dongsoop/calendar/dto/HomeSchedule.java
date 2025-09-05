package com.dongsoop.dongsoop.calendar.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record HomeSchedule(

        String title,
        LocalTime startAt,
        LocalTime endAt
) {
    public HomeSchedule(String title, LocalDateTime startAt, LocalDateTime endAt) {
        this(title, startAt.toLocalTime(), endAt.toLocalTime());
    }

    public HomeSchedule(String title, LocalDate startAt, LocalDate endAt) {
        this(title, LocalTime.MIN, LocalTime.MAX);
    }
}
