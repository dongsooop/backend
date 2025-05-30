package com.dongsoop.dongsoop.study.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudyBoardOverview {

    private Long id;

    private Integer volunteer;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String title;

    private String content;

    private String tags;
}
