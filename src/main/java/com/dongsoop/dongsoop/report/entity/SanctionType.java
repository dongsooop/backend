package com.dongsoop.dongsoop.report.entity;

import com.dongsoop.dongsoop.report.exception.SanctionEndDateRequiredException;
import java.time.LocalDateTime;

public enum SanctionType {
    WARNING("경고"),
    TEMPORARY_BAN("일시정지"),
    PERMANENT_BAN("영구정지"),
    CONTENT_DELETION("게시글 삭제"),
    CHAT_KICK("채팅방 추방");

    // 경고는 영구 누적이 정책이라 종료일로 만료시키지 않는다
    public static final LocalDateTime PERMANENT_END_DATE = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final String description;

    SanctionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBan() {
        return this == TEMPORARY_BAN || this == PERMANENT_BAN;
    }

    public LocalDateTime resolveEndDate(LocalDateTime requestedEndDate, LocalDateTime startDate) {
        if (requestedEndDate != null) {
            return requestedEndDate;
        }

        return switch (this) {
            case TEMPORARY_BAN -> throw new SanctionEndDateRequiredException();
            case WARNING, PERMANENT_BAN -> PERMANENT_END_DATE;
            case CONTENT_DELETION, CHAT_KICK -> startDate;
        };
    }
}
