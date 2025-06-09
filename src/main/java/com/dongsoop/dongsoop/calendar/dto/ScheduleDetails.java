package com.dongsoop.dongsoop.calendar.dto;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleDetails {

    private Long id;

    private String title;

    private String location;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private ScheduleType type;
}
