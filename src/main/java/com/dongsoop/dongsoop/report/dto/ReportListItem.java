package com.dongsoop.dongsoop.report.dto;

/** 신고 목록의 요약·상세 응답이 공유하는 반환 타입. */
public sealed interface ReportListItem permits ReportResponse, ReportSummaryResponse {
}
