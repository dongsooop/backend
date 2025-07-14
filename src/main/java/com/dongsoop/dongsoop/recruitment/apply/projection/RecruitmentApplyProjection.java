package com.dongsoop.dongsoop.recruitment.apply.projection;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.querydsl.core.types.ConstructorExpression;

public interface RecruitmentApplyProjection {

    ConstructorExpression<ApplyDetails> getApplyDetailsExpression();
}
