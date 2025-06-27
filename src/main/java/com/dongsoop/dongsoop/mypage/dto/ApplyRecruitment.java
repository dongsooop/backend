package com.dongsoop.dongsoop.mypage.dto;

import java.time.LocalDateTime;

public interface ApplyRecruitment {

    Long getId();

    Long getVolunteer();

    LocalDateTime getStartAt();

    LocalDateTime getEndAt();

    String getTitle();

    String getContent();

    String getTags();

    String getDepartmentTypeList();

    String getBoardType();

    LocalDateTime getCreatedAt();

    boolean getIsRecruiting();
}
