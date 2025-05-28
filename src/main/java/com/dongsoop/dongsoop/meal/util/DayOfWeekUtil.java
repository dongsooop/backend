package com.dongsoop.dongsoop.meal.util;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class DayOfWeekUtil {

    private static final Map<DayOfWeek, String> KOREAN_DAY_NAMES = new EnumMap<>(DayOfWeek.class);
    private static final Set<DayOfWeek> WEEKDAYS = Set.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
    );

    static {
        KOREAN_DAY_NAMES.put(DayOfWeek.MONDAY, "월");
        KOREAN_DAY_NAMES.put(DayOfWeek.TUESDAY, "화");
        KOREAN_DAY_NAMES.put(DayOfWeek.WEDNESDAY, "수");
        KOREAN_DAY_NAMES.put(DayOfWeek.THURSDAY, "목");
        KOREAN_DAY_NAMES.put(DayOfWeek.FRIDAY, "금");
        KOREAN_DAY_NAMES.put(DayOfWeek.SATURDAY, "토");
        KOREAN_DAY_NAMES.put(DayOfWeek.SUNDAY, "일");
    }

    public static String toKorean(DayOfWeek dayOfWeek) {
        return KOREAN_DAY_NAMES.getOrDefault(dayOfWeek, "");
    }

    public static boolean isWeekday(DayOfWeek dayOfWeek) {
        return WEEKDAYS.contains(dayOfWeek);
    }
}