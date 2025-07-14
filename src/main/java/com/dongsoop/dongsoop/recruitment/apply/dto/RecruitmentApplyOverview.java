package com.dongsoop.dongsoop.recruitment.apply.dto;

import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;

public interface RecruitmentApplyOverview {

    Long getMemberId();

    String getMemberName();

    RecruitmentApplyStatus getStatus();

    String getDepartmentName();
}
