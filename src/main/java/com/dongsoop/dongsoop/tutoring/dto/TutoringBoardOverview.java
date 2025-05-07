package com.dongsoop.dongsoop.tutoring.dto;

import java.time.LocalDateTime;

public interface TutoringBoardOverview {

    Integer getCapacity();

    LocalDateTime getEndAt();

    String getTitle();

    String getContent();

    String getTags();
}
