package com.dongsoop.dongsoop.eclass.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EclassAssignmentResponse {

    private Long id;
    private String eclassId;
    private String courseId;
    private String courseName;
    private String title;
    private String dueDate;
    private boolean isSubmitted;
    private String status;
    private String link;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
