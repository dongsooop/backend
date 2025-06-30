package com.dongsoop.dongsoop.recruitment.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import java.time.LocalDateTime;

public interface RecruitmentRepositoryUtils {

    BooleanExpression isRecruiting(DateTimePath<LocalDateTime> startAt,
                                   DateTimePath<LocalDateTime> endAt,
                                   LocalDateTime now);
}
