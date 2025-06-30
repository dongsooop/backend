package com.dongsoop.dongsoop.report.entity;

public enum ReportFilterType {
    ALL,           // 전체
    UNPROCESSED,   // 미처리 (isProcessed = false)
    PROCESSED,     // 처리완료 (isProcessed = true)
    ACTIVE_SANCTIONS // 활성 제재 (isSanctionActive = true)
}

