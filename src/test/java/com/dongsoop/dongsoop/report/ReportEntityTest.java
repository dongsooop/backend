package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReportEntityTest {

    @Test
    @DisplayName("새 신고는 자동 판정 전 상태로 만들어진다")
    void newReport_IsNotAutoReviewed() {
        Report report = Report.builder().reportType(ReportType.CHAT_MESSAGE).build();

        assertThat(report.getIsAutoReviewed()).isFalse();
    }

    @Test
    @DisplayName("자동 판정 표시는 처리 완료로 바꾸지 않는다")
    void markAutoReviewed_KeepsUnprocessed() {
        Report report = Report.builder().reportType(ReportType.CHAT_MESSAGE).build();

        report.markAutoReviewed();

        assertThat(report.getIsAutoReviewed()).isTrue();
        assertThat(report.getIsProcessed()).isFalse();
    }

    @Test
    @DisplayName("메시지 신고 타입만 isMessageReport가 참이다")
    void isMessageReport() {
        assertThat(ReportType.CHAT_MESSAGE.isMessageReport()).isTrue();
        assertThat(ReportType.PROJECT_BOARD.isMessageReport()).isFalse();
        assertThat(ReportType.MEMBER.isMessageReport()).isFalse();
    }
}
