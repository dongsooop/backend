package com.dongsoop.dongsoop.eclass.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EclassAssignmentItem {

    @NotBlank(message = "이클래스 과제 ID를 입력해주세요.")
    private String eclassId;

    private String courseId;

    @NotBlank(message = "수업명을 입력해주세요.")
    private String courseName;

    @NotBlank(message = "과제명을 입력해주세요.")
    private String title;

    private String dueDate;

    private boolean isSubmitted;

    private String status;

    private String link;
}