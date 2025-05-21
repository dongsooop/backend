package com.dongsoop.dongsoop.tutoring.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TutoringBoardDetails {

    private Long id;

    private String title;

    private String content;

    private String tags;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private DepartmentType departmentType;

    private String author;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer volunteer;
}
