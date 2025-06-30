package com.dongsoop.dongsoop.recruitment.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class RecruitmentRepositoryUtilsImpl implements RecruitmentRepositoryUtils {

    public BooleanExpression isRecruiting(DateTimePath<LocalDateTime> startAt,
                                          DateTimePath<LocalDateTime> endAt,
                                          LocalDateTime now) {
        return endAt.gt(now)
                .and(startAt.lt(now));
    }
}
