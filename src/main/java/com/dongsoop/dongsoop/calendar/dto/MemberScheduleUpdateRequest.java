package com.dongsoop.dongsoop.calendar.dto;

import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^[A-Fa-f0-9]{6}$", message = "색상은 # 없이 6자리 HEX 형식이어야 합니다 (예: FFFFFF)")
    private String color;
}
