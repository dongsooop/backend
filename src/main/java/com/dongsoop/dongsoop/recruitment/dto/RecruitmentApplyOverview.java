package com.dongsoop.dongsoop.recruitment.dto;

import com.dongsoop.dongsoop.recruitment.entity.RecruitmentApplyStatus;

public interface RecruitmentApplyOverview {

    Long getMemberId();

    String getMemberName();

    RecruitmentApplyStatus getStatus();

    String getDepartmentName();
}
