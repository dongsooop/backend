package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.handler.ContentDeletionHandler;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.service.AsyncAutoSanctionService;
import com.dongsoop.dongsoop.report.service.BoardContentService;
import com.dongsoop.dongsoop.report.service.SanctionExecutor;
import com.dongsoop.dongsoop.report.service.TextFilteringService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AsyncAutoSanctionServiceTest {

    @InjectMocks
    private AsyncAutoSanctionService asyncAutoSanctionService;

    @Mock
    private SanctionRepository sanctionRepository;
    @Mock
    private ContentDeletionHandler contentDeletionHandler;
    @Mock
    private BoardContentService boardContentService;
    @Mock
    private TextFilteringService textFilteringService;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private SanctionExecutor sanctionExecutor;

    private final Member target = Member.builder().id(2L).build();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(asyncAutoSanctionService, "systemAdminId", 0L);
    }

    @Test
    @DisplayName("게시글 욕설 자동 제재는 제재 대상 회원을 채워 저장한다")
    void processReportAsync_BoardProfanity_FillsTargetMember() {
        Report report = Report.builder().id(1L).reportType(ReportType.PROJECT_BOARD).targetId(10L)
                .targetMember(target).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(boardContentService.getTitle(10L, ReportType.PROJECT_BOARD)).thenReturn("제목");
        when(boardContentService.getContent(10L, ReportType.PROJECT_BOARD)).thenReturn("본문");
        when(textFilteringService.hasProfanity("제목", "", "본문")).thenReturn(true);

        asyncAutoSanctionService.processReportAsync(report);

        ArgumentCaptor<Sanction> captor = ArgumentCaptor.forClass(Sanction.class);
        verify(sanctionRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetMember()).isEqualTo(target);
        assertThat(report.getIsProcessed()).isTrue();
    }

    @Test
    @DisplayName("욕설인 채팅 신고는 자동 경고를 주고 누적 검사를 실행한다")
    void processReportAsync_ChatProfanity_WarnsAndChecksAccumulation() {
        Report report = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).messageId("m1").messageContent("욕설").build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(textFilteringService.hasProfanity("", "", "욕설")).thenReturn(true);

        asyncAutoSanctionService.processReportAsync(report);

        ArgumentCaptor<Sanction> captor = ArgumentCaptor.forClass(Sanction.class);
        verify(sanctionRepository).save(captor.capture());
        Sanction warning = captor.getValue();
        assertThat(warning.getSanctionType()).isEqualTo(SanctionType.WARNING);
        assertThat(warning.getEndDate()).isEqualTo(SanctionType.PERMANENT_END_DATE);
        assertThat(warning.getTargetMember()).isEqualTo(target);
        assertThat(report.getIsProcessed()).isTrue();
        verify(sanctionExecutor).checkWarningAccumulation(target);
    }

    @Test
    @DisplayName("욕설이 아니거나 필터 호출이 실패한 채팅 신고는 닫지 않고 관리자에게 넘긴다")
    void processReportAsync_ChatClean_LeavesForAdmin() {
        Report report = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).messageContent("안녕").build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(textFilteringService.hasProfanity("", "", "안녕")).thenReturn(false);

        asyncAutoSanctionService.processReportAsync(report);

        assertThat(report.getIsProcessed()).isFalse();
        assertThat(report.getIsAutoReviewed()).isTrue();
        verify(reportRepository).save(report);
        verify(sanctionRepository, never()).save(any());
        verify(contentDeletionHandler, never()).deleteContent(any());
    }

    @Test
    @DisplayName("같은 메시지에 이미 경고가 있으면 욕설이어도 새 경고 없이 처리 완료로 닫는다")
    void processReportAsync_ChatProfanityAlreadyWarned_ClosesWithoutWarning() {
        Report report = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(2L)
                .targetMember(target).messageId("m1").messageContent("욕설").build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(textFilteringService.hasProfanity("", "", "욕설")).thenReturn(true);
        when(reportRepository.existsByMessageIdAndSanctionSanctionType("m1", SanctionType.WARNING))
                .thenReturn(true);

        asyncAutoSanctionService.processReportAsync(report);

        assertThat(report.getIsProcessed()).isTrue();
        assertThat(report.getSanction()).isNull();
        verify(reportRepository).save(report);
        verify(sanctionRepository, never()).save(any());
        verify(sanctionExecutor, never()).checkWarningAccumulation(any());
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
        verify(sanctionRepository, never()).save(any());
        verify(reportRepository, never()).save(any());
    }
}
