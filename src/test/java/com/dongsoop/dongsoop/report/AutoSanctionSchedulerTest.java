package com.dongsoop.dongsoop.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.scheduler.AutoSanctionScheduler;
import com.dongsoop.dongsoop.report.service.AsyncAutoSanctionService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;

@ExtendWith(MockitoExtension.class)
class AutoSanctionSchedulerTest {

    @InjectMocks
    private AutoSanctionScheduler scheduler;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private AsyncAutoSanctionService asyncAutoSanctionService;

    private static Report chatReport(long id, String messageId) {
        return Report.builder().id(id).reportType(ReportType.CHAT_MESSAGE).targetId(2L).messageId(messageId).build();
    }

    @Test
    @DisplayName("같은 메시지 신고가 한 배치에 여러 건이면 한 번만 처리한다")
    void processAutoSanctions_SameMessageInBatch_ProcessesOnce() {
        when(reportRepository.findUnprocessedReports(any()))
                .thenReturn(List.of(chatReport(1L, "m1"), chatReport(2L, "m1"), chatReport(3L, "m1")));
        when(asyncAutoSanctionService.processReportAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        scheduler.processAutoSanctions();

        verify(asyncAutoSanctionService, times(1)).processReportAsync(any());
    }

    @Test
    @DisplayName("같은 회원의 서로 다른 메시지 신고는 각각 처리한다")
    void processAutoSanctions_DifferentMessagesOfSameMember_ProcessesEach() {
        when(reportRepository.findUnprocessedReports(any()))
                .thenReturn(List.of(chatReport(1L, "m1"), chatReport(2L, "m2")));
        when(asyncAutoSanctionService.processReportAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        scheduler.processAutoSanctions();

        verify(asyncAutoSanctionService, times(2)).processReportAsync(any());
    }

    @Test
    @DisplayName("실행기가 제출을 거절해도 처리 중 키를 비워 다음 주기에 같은 메시지를 다시 처리한다")
    void processAutoSanctions_SubmissionRejected_ReleasesKey() {
        when(reportRepository.findUnprocessedReports(any())).thenReturn(List.of(chatReport(1L, "m1")));
        when(asyncAutoSanctionService.processReportAsync(any()))
                .thenThrow(new TaskRejectedException("queue full"))
                .thenReturn(CompletableFuture.completedFuture(null));

        scheduler.processAutoSanctions();
        scheduler.processAutoSanctions();

        verify(asyncAutoSanctionService, times(2)).processReportAsync(any());
    }
}
