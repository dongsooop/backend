package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.dongsoop.dongsoop.report.dto.CreateReportRequest;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.UnsupportedReportTypeException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.ReportServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportCreateGuardTest {

    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock
    private ReportRepository reportRepository;

    @Test
    @DisplayName("채팅 메시지 신고는 게시판 신고 API로 받을 수 없다")
    void createReport_ChatMessageType_Throws() {
        CreateReportRequest request = new CreateReportRequest(ReportType.CHAT_MESSAGE, 1L, ReportReason.SPAM, null);

        assertThatThrownBy(() -> reportService.createReport(request))
                .isInstanceOf(UnsupportedReportTypeException.class);
        verify(reportRepository, never()).save(any());
    }
}
