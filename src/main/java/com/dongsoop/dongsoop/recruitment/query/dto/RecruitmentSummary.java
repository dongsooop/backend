package com.dongsoop.dongsoop.recruitment.query.dto;

import com.dongsoop.dongsoop.recruitment.RecruitmentType;
import java.time.LocalDateTime;

public interface RecruitmentSummary {

    Long getId();

    Long getVolunteer();

    LocalDateTime getStartAt();

    LocalDateTime getEndAt();

    String getTitle();

    String getContent();

    String getTags();

    String getDepartmentTypeList();

    RecruitmentType getBoardType();

    LocalDateTime getCreatedAt();

    RecruitmentStatusType getStatus();
}
