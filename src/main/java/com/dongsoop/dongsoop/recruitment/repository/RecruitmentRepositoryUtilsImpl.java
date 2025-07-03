package com.dongsoop.dongsoop.recruitment.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DateTimePath;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecruitmentRepositoryUtilsImpl implements RecruitmentRepositoryUtils {

    private final Clock clock;

    public BooleanExpression isRecruiting(DateTimePath<LocalDateTime> startAt,
                                          DateTimePath<LocalDateTime> endAt) {
        LocalDateTime now = LocalDateTime.now(clock);

        return endAt.gt(now)
                .and(startAt.lt(now));
    }
}
