package com.dongsoop.dongsoop.calendar.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.ComparableExpression;

public class ScheduleDurationChecker {

    private ScheduleDurationChecker() {
    }

    public static <T extends Comparable<? super T>> BooleanExpression isDurationWithin(
            ComparableExpression<T> scheduleStart,
            ComparableExpression<T> scheduleEnd,
            T startAt,
            T endAt) {
        BooleanExpression startsWithinDuration = scheduleStart.goe(startAt)
                .and(scheduleStart.lt(endAt));

        BooleanExpression endsWithinDuration = scheduleEnd.goe(startAt)
                .and(scheduleEnd.lt(endAt));

        BooleanExpression overlapAllDuration = scheduleStart.lt(startAt)
                .and(scheduleEnd.gt(endAt));

        return startsWithinDuration
                .or(endsWithinDuration)
                .or(overlapAllDuration);
    }
}
