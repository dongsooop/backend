package com.dongsoop.dongsoop.report.entity;

public enum SanctionType {
    WARNING("경고"),
    TEMPORARY_BAN("일시정지"),
    PERMANENT_BAN("영구정지"),
    CONTENT_DELETION("게시글 삭제");

    private final String description;

    SanctionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}