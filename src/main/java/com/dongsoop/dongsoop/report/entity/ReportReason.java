package com.dongsoop.dongsoop.report.entity;

public enum ReportReason {
    SPAM("스팸/도배"),
    INAPPROPRIATE_CONTENT("부적절한 내용"),
    HATE_SPEECH("혐오 발언"),
    FRAUD("사기/허위 정보"),
    PRIVACY_VIOLATION("개인정보 침해"),
    COPYRIGHT_INFRINGEMENT("저작권 침해"),
    OTHER("기타");

    private final String description;

    ReportReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}