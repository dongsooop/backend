package com.dongsoop.dongsoop.recruitment.board.projection;

import com.dongsoop.dongsoop.recruitment.RecruitmentViewType;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.querydsl.core.types.ConstructorExpression;

public interface RecruitmentProjection {

    ConstructorExpression<RecruitmentOverview> getRecruitmentOverviewExpression();

    ConstructorExpression<RecruitmentDetails> getRecruitmentDetailsExpression(RecruitmentViewType viewType,
                                                                              boolean isAlreadyApplied);
}
