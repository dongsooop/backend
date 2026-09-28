package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.ReportAlreadyProcessedException;
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
        assertThat(ReportType.BLINDDATE_MESSAGE.isMessageReport()).isTrue();
        assertThat(ReportType.PROJECT_BOARD.isMessageReport()).isFalse();
        assertThat(ReportType.MEMBER.isMessageReport()).isFalse();
    }

    @Test
    @DisplayName("기각하면 제재 없이 처리 완료되고 처리한 관리자가 남는다")
    void dismiss_ClosesWithoutSanction() {
        Member admin = Member.builder().id(100L).build();
        Report report = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).build();

        report.dismiss(admin);

        assertThat(report.getIsProcessed()).isTrue();
        assertThat(report.getAdmin()).isEqualTo(admin);
        assertThat(report.getSanction()).isNull();
    }

    @Test
    @DisplayName("이미 처리된 신고는 기각할 수 없다")
    void dismiss_AlreadyProcessed_Throws() {
        Report report = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).isProcessed(true).build();

        assertThatThrownBy(() -> report.dismiss(Member.builder().id(100L).build()))
                .isInstanceOf(ReportAlreadyProcessedException.class);
    }

    @Test
    @DisplayName("게시판 신고 타입 집합은 바꿀 수 없다")
    void boardTypes_IsUnmodifiable() {
        assertThatThrownBy(() -> ReportType.BOARD_TYPES.add(ReportType.MEMBER))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
