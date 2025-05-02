package com.dongsoop.dongsoop.tutoring.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TutoringBoardOverview {

    private Integer recruitmentCapacity;

    private LocalDateTime recruitmentDeadline;

    private String title;

    private String content;

    private String tags;
}
