package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.handler.ContentDeletionHandler;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.service.AsyncAutoSanctionService;
import com.dongsoop.dongsoop.report.service.BoardContentService;
import com.dongsoop.dongsoop.report.service.TextFilteringService;
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
        when(boardContentService.getTitle(10L, ReportType.PROJECT_BOARD)).thenReturn("제목");
        when(boardContentService.getContent(10L, ReportType.PROJECT_BOARD)).thenReturn("본문");
        when(textFilteringService.hasProfanity("제목", "", "본문")).thenReturn(true);

        asyncAutoSanctionService.processReportAsync(report);

        ArgumentCaptor<Sanction> captor = ArgumentCaptor.forClass(Sanction.class);
        verify(sanctionRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetMember()).isEqualTo(target);
        assertThat(report.getIsProcessed()).isTrue();
    }
}
