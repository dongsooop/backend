package com.dongsoop.dongsoop.meal.util;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.Map;

public class DayOfWeekUtil {

    private static final Map<DayOfWeek, String> KOREAN_DAY_NAMES = new EnumMap<>(DayOfWeek.class);

    static {
        KOREAN_DAY_NAMES.put(DayOfWeek.MONDAY, "월");
        KOREAN_DAY_NAMES.put(DayOfWeek.TUESDAY, "화");
        KOREAN_DAY_NAMES.put(DayOfWeek.WEDNESDAY, "수");
        KOREAN_DAY_NAMES.put(DayOfWeek.THURSDAY, "목");
        KOREAN_DAY_NAMES.put(DayOfWeek.FRIDAY, "금");
        KOREAN_DAY_NAMES.put(DayOfWeek.SATURDAY, "토");
        KOREAN_DAY_NAMES.put(DayOfWeek.SUNDAY, "일");
    }

    private DayOfWeekUtil() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }

    public static String toKorean(DayOfWeek dayOfWeek) {
        return KOREAN_DAY_NAMES.getOrDefault(dayOfWeek, "");
    }
}