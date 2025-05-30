package com.dongsoop.dongsoop.calendar.dto;

import com.dongsoop.dongsoop.calendar.entity.MemberSchedule;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemberScheduleRequest {

    @NotNull
    @Length(min = 1, max = 60)
    private String title;

    @NotNull
    private String location;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    public MemberSchedule toEntity() {
        return MemberSchedule.builder()
                .title(title)
                .location(location)
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }
}
