package com.dongsoop.dongsoop.project.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectBoardOverview {

    private Long id;

    private Integer volunteer;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private String title;

    private String content;

    private String tags;
}
