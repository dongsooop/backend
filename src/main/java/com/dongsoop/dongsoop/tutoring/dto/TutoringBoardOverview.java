package com.dongsoop.dongsoop.tutoring.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TutoringBoardOverview {

    private Long id;

    private Integer capacity;

    private Integer volunteer;

    private LocalDateTime endAt;

    private String title;

    private String content;

    private String tags;

    private LocalDateTime createdAt;
}
