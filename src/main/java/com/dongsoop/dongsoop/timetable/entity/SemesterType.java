package com.dongsoop.dongsoop.timetable.entity;

import com.dongsoop.dongsoop.timetable.exception.NotAvailableMonthException;

public enum SemesterType {

    FIRST,
    SECOND,
    SUMMER,
    WINTER;

    public static SemesterType fromMonth(int month) {
        if (month == 1) {
            return WINTER;
        }

        if (month == 7) {
            return SUMMER;
        }

        if (month >= 3 && month <= 6) {
            return FIRST;
        }

        if (month >= 9 && month <= 12) {
            return SECOND;
        }

        throw new NotAvailableMonthException(month);
    }
}
