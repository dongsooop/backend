package com.dongsoop.dongsoop.tutoring.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.time.LocalDateTime;

public interface TutoringBoardDetails {

    Long getId();

    String getTitle();

    String getContent();

    String getTags();

    Integer getCapacity();

    LocalDateTime getStartAt();

    LocalDateTime getEndAt();

    DepartmentType getDepartmentType();

    String getAuthor();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}
