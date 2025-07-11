package com.dongsoop.dongsoop.recruitment.projection;

import com.dongsoop.dongsoop.mypage.dto.MyRecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentOverview;
import com.querydsl.core.types.ConstructorExpression;

public interface RecruitmentProjection {

    ConstructorExpression<MyRecruitmentOverview> getApplyRecruitmentExpression();

    ConstructorExpression<MyRecruitmentOverview> getOpenedRecruitmentExpression();

    ConstructorExpression<RecruitmentOverview> getRecruitmentOverviewExpression();

    ConstructorExpression<RecruitmentDetails> getRecruitmentDetailsExpression(RecruitmentViewType viewType,
                                                                              boolean isAlreadyApplied);
}
