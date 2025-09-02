package com.dongsoop.dongsoop.timetable.entity;

import com.dongsoop.dongsoop.timetable.exception.NotAvailableMonthException;

public enum SemesterType {

    FIRST,
    SECOND,
    SUMMER,
    WINTER;

    private static final int WINTER_MONTH = 1;
    private static final int SUMMER_MONTH = 7;
    private static final int FIRST_SEMESTER_START_MONTH = 3;
    private static final int FIRST_SEMESTER_END_MONTH = 6;
    private static final int SECOND_SEMESTER_START_MONTH = 9;
    private static final int SECOND_SEMESTER_END_MONTH = 12;

    public static SemesterType fromMonth(int month) {
        if (month == WINTER_MONTH) {
            return WINTER;
        }

        if (month == SUMMER_MONTH) {
            return SUMMER;
        }

        if (month >= FIRST_SEMESTER_START_MONTH && month <= FIRST_SEMESTER_END_MONTH) {
            return FIRST;
        }

        if (month >= SECOND_SEMESTER_START_MONTH && month <= SECOND_SEMESTER_END_MONTH) {
            return SECOND;
        }

        throw new NotAvailableMonthException(month);
    }
}
