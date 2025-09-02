package com.dongsoop.dongsoop.timetable.dto;

import java.time.LocalTime;

public record TimetableNotificationDto(

        String name,
        LocalTime startAt,
        Long memberId
) {
}
