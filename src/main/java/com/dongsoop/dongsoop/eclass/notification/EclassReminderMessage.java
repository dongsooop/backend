package com.dongsoop.dongsoop.eclass.notification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class EclassReminderMessage {

    private static final String TITLE_FORMAT = "[%s] 과제 %d일 전입니다";
    private static final String TITLE_TODAY_FORMAT = "[%s] 과제 오늘 마감입니다";
    private static final String BODY_FORMAT = "%s · 마감 %s";
    private static final String TITLE_DUE_CHANGED_FORMAT = "[%s] 과제 마감이 앞당겨졌어요";
    private static final String ELLIPSIS = "…";
    private static final DateTimeFormatter DUE_FORMATTER =
            DateTimeFormatter.ofPattern("M월 d일 (E) HH:mm", Locale.KOREAN);

    private EclassReminderMessage() {
    }

    public static String title(String courseName, int daysBefore, int courseNameMaxLength) {
        String shortened = shorten(courseName, courseNameMaxLength);

        if (daysBefore == 0) {
            return String.format(TITLE_TODAY_FORMAT, shortened);
        }

        return String.format(TITLE_FORMAT, shortened, daysBefore);
    }

    public static String dueDateChangedTitle(String courseName, int courseNameMaxLength) {
        return String.format(TITLE_DUE_CHANGED_FORMAT, shorten(courseName, courseNameMaxLength));
    }

    /**
     * 마감 시각은 이클래스 화면과 같은 값을 그대로 보여준다 — "전날 밤까지"처럼 바꿔 쓰면 혼란을 준다.
     */
    public static String body(String assignmentTitle, LocalDateTime dueAt) {
        return String.format(BODY_FORMAT, assignmentTitle, DUE_FORMATTER.format(dueAt));
    }

    private static String shorten(String courseName, int maxLength) {
        if (courseName.length() <= maxLength) {
            return courseName;
        }

        return courseName.substring(0, maxLength) + ELLIPSIS;
    }
}
