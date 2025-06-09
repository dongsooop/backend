package com.dongsoop.dongsoop.calendar.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberScheduleUpdateRequest {

    @Length(min = 1, max = 60)
    private String title;

    @Length(max = 20)
    private String location;

    private LocalDateTime startAt;

    private LocalDateTime endAt;
}
