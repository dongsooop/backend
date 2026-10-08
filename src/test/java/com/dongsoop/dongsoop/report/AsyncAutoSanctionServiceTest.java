package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.AsyncAutoSanctionService;
import com.dongsoop.dongsoop.report.service.BoardContentService;
import com.dongsoop.dongsoop.report.service.SanctionExecutor;
import com.dongsoop.dongsoop.report.service.TextFilteringService;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class AsyncAutoSanctionServiceTest {

    @InjectMocks
    private AsyncAutoSanctionService asyncAutoSanctionService;

    @Mock
    private BoardContentService boardContentService;
    @Mock
    private TextFilteringService textFilteringService;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private SanctionExecutor sanctionExecutor;

    @Mock
    private TransactionTemplate transactionTemplate;

    private final Member target = Member.builder().id(2L).build();

    @BeforeEach
    void runTransactionCallbackInline() {
        lenient().doAnswer(invocation -> {
            invocation.<Consumer<TransactionStatus>>getArgument(0).accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    private void stubReads(Report report) {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        lenient().when(reportRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(report));
    }

    private Report boardReport() {
        Report report = Report.builder().id(1L).reportType(ReportType.PROJECT_BOARD).targetId(10L)
                .targetMember(target).build();
        stubReads(report);
        when(boardContentService.getTitle(10L, ReportType.PROJECT_BOARD)).thenReturn("제목");
        when(boardContentService.getContent(10L, ReportType.PROJECT_BOARD)).thenReturn("본문");
        return report;
    }

    @Test
    @DisplayName("게시글 욕설이면 시스템 명의 게시글 삭제 제재를 요청한다")
    void processReportAsync_BoardProfanity_IssuesContentDeletion() {
        Report report = boardReport();
        when(textFilteringService.hasProfanity("제목", "", "본문")).thenReturn(true);

        asyncAutoSanctionService.processReportAsync(report);

        verify(sanctionExecutor).issueBySystem(report, SanctionType.CONTENT_DELETION, "부적절한 언어 사용",
                "자동 제재에 의한 게시글 삭제");
    }

    @Test
    @DisplayName("게시글 욕설이 아니면 제재 없이 처리 완료로 닫는다")
    void processReportAsync_BoardClean_ClosesWithoutSanction() {
        Report report = boardReport();
        when(textFilteringService.hasProfanity("제목", "", "본문")).thenReturn(false);

        asyncAutoSanctionService.processReportAsync(report);

        assertThat(report.getIsProcessed()).isTrue();
        verify(sanctionExecutor, never()).issueBySystem(any(), any(), any(), any());
    }

    @Test
    @DisplayName("욕설인 채팅 신고는 시스템 명의 경고를 요청한다")
    void processReportAsync_ChatProfanity_IssuesWarning() {
        Report report = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).messageId("m1").messageContent("욕설").build();
        stubReads(report);
        when(textFilteringService.hasProfanity("", "", "욕설")).thenReturn(true);

        asyncAutoSanctionService.processReportAsync(report);

        verify(sanctionExecutor).issueBySystem(report, SanctionType.WARNING, "부적절한 언어 사용", "자동 제재에 의한 경고");
    }

    @Test
    @DisplayName("욕설이 아니거나 필터 호출이 실패한 채팅 신고는 닫지 않고 관리자에게 넘긴다")
    void processReportAsync_ChatClean_LeavesForAdmin() {
        Report report = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).messageContent("안녕").build();
        stubReads(report);
        when(textFilteringService.hasProfanity("", "", "안녕")).thenReturn(false);

        asyncAutoSanctionService.processReportAsync(report);

        assertThat(report.getIsProcessed()).isFalse();
        assertThat(report.getIsAutoReviewed()).isTrue();
        verify(sanctionExecutor, never()).issueBySystem(any(), any(), any(), any());
    }

    @Test
    @DisplayName("같은 메시지에 이미 경고가 있으면 욕설이어도 새 경고 없이 처리 완료로 닫는다")
    void processReportAsync_ChatProfanityAlreadyWarned_ClosesWithoutWarning() {
        Report report = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).messageId("m1").messageContent("욕설").build();
        stubReads(report);
        when(textFilteringService.hasProfanity("", "", "욕설")).thenReturn(true);
        when(reportRepository.existsByMessageIdAndSanctionSanctionType("m1", SanctionType.WARNING))
                .thenReturn(true);

        asyncAutoSanctionService.processReportAsync(report);

        assertThat(report.getIsProcessed()).isTrue();
        assertThat(report.getSanction()).isNull();
        verify(sanctionExecutor, never()).issueBySystem(any(), any(), any(), any());
    }

    @Test
    @DisplayName("다시 읽은 신고가 이미 처리됐으면 아무것도 하지 않는다")
    void processReportAsync_AlreadyProcessedOnReload_Skips() {
        Report detached = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).messageId("m1").messageContent("욕설").build();
        Report reloaded = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).messageId("m1").messageContent("욕설").isProcessed(true).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(reloaded));

        asyncAutoSanctionService.processReportAsync(detached);

        verify(textFilteringService, never()).hasProfanity(any(), any(), any());
        verify(sanctionExecutor, never()).issueBySystem(any(), any(), any(), any());
    }

    @Test
    @DisplayName("필터를 기다리는 동안 관리자가 기각했으면 잠금 조회에서 처리됨을 보고 자동 제재하지 않는다")
    void processReportAsync_DismissedDuringFiltering_SkipsSanction() {
        Report unprocessed = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).messageId("m1").messageContent("욕설").build();
        Report dismissed = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).messageId("m1").messageContent("욕설").isProcessed(true).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(unprocessed));
        when(reportRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(dismissed));
        when(textFilteringService.hasProfanity("", "", "욕설")).thenReturn(true);

        asyncAutoSanctionService.processReportAsync(unprocessed);

        assertThat(dismissed.getIsAutoReviewed()).isFalse();
        verify(sanctionExecutor, never()).issueBySystem(any(), any(), any(), any());
    }
}
