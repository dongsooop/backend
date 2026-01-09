package com.dongsoop.dongsoop.notification.constant;

public enum NotificationType {

    CHAT(true),
    NOTICE(true),
    RECRUITMENT_STUDY_APPLY(true),
    RECRUITMENT_PROJECT_APPLY(true),
    RECRUITMENT_TUTORING_APPLY(true),
    RECRUITMENT_STUDY_APPLY_RESULT(true),
    RECRUITMENT_PROJECT_APPLY_RESULT(true),
    RECRUITMENT_TUTORING_APPLY_RESULT(true),
    TIMETABLE(true),
    CALENDAR(true),
    MARKETING(false),
    FEEDBACK(true),
    BLINDDATE(true);

    private final boolean defaultActiveState;

    NotificationType(boolean defaultActiveState) {
        this.defaultActiveState = defaultActiveState;
    }

    public boolean getDefaultActiveState() {
        return this.defaultActiveState;
    }
}
