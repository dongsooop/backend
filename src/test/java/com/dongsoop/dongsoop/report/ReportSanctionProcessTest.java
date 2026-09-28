package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.exception.GroupChatOnlyException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.exception.SanctionEndDateRequiredException;
import com.dongsoop.dongsoop.report.exception.SanctionTargetMismatchException;
import com.dongsoop.dongsoop.report.exception.UnsupportedSanctionTypeException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.service.ReportServiceImpl;
import com.dongsoop.dongsoop.report.service.SanctionExecutor;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportSanctionProcessTest {

    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberService memberService;
    @Mock
    private SanctionRepository sanctionRepository;
    @Mock
    private SanctionExecutor sanctionExecutor;

    private final Member admin = Member.builder().id(100L).build();
    private final Member target = Member.builder().id(2L).build();
    private Report report;

    @BeforeEach
    void setUp() {
        report = Report.builder().id(1L).reportType(ReportType.MEMBER).targetId(2L).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(memberRepository.findById(2L)).thenReturn(Optional.of(target));
        when(memberService.getMemberReferenceByContext()).thenReturn(admin);
    }

    private Sanction capturedSanction() {
        ArgumentCaptor<Sanction> captor = ArgumentCaptor.forClass(Sanction.class);
        verify(sanctionRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("관리자 제재는 관리자·대상·신고·사유를 채워 저장한다")
    void processSanction_FillsRequiredFields() {
        reportService.processSanction(new ProcessSanctionRequest(1L, 2L, SanctionType.WARNING, null, null));

        Sanction sanction = capturedSanction();
        assertThat(sanction.getAdmin()).isEqualTo(admin);
        assertThat(sanction.getMember()).isEqualTo(target);
        assertThat(sanction.getTargetMember()).isEqualTo(target);
        assertThat(sanction.getReport()).isEqualTo(report);
        assertThat(sanction.getReason()).isEqualTo("경고");
        assertThat(report.getIsProcessed()).isTrue();
    }

    @Test
    @DisplayName("경고와 영구정지는 종료일이 없으면 영구 종료일로 저장한다")
    void processSanction_WarningWithoutEndDate_UsesPermanentEndDate() {
        reportService.processSanction(new ProcessSanctionRequest(1L, 2L, SanctionType.WARNING, "욕설", null));

        assertThat(capturedSanction().getEndDate()).isEqualTo(SanctionType.PERMANENT_END_DATE);
    }

    @Test
    @DisplayName("일시정지는 종료일이 없으면 거절하고 저장하지 않는다")
    void processSanction_TemporaryBanWithoutEndDate_Throws() {
        ProcessSanctionRequest request = new ProcessSanctionRequest(1L, 2L, SanctionType.TEMPORARY_BAN, "욕설", null);

        assertThatThrownBy(() -> reportService.processSanction(request))
                .isInstanceOf(SanctionEndDateRequiredException.class);
        verify(sanctionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("일시정지는 입력한 종료일로 저장한다")
    void processSanction_TemporaryBanWithEndDate_UsesRequestedEndDate() {
        LocalDateTime endAt = LocalDateTime.of(2026, 10, 5, 0, 0);

        reportService.processSanction(new ProcessSanctionRequest(1L, 2L, SanctionType.TEMPORARY_BAN, "욕설", endAt));

        assertThat(capturedSanction().getEndDate()).isEqualTo(endAt);
    }

    @Test
    @DisplayName("채팅 메시지 신고가 아니면 채팅방 추방을 거절한다")
    void processSanction_ChatKickOnNonChatReport_Throws() {
        ProcessSanctionRequest request = new ProcessSanctionRequest(1L, 2L, SanctionType.CHAT_KICK, null, null);

        assertThatThrownBy(() -> reportService.processSanction(request))
                .isInstanceOf(GroupChatOnlyException.class);
        verify(sanctionRepository, never()).saveAndFlush(any());
    }

    private void stubChatMessageReport(Member reportedMember) {
        Report chatReport = Report.builder().id(1L).reportType(ReportType.CHAT_MESSAGE).targetId(reportedMember.getId())
                .targetMember(reportedMember).messageId("m1").build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(chatReport));
    }

    @Test
    @DisplayName("메시지 신고는 신고된 메시지 작성자가 아닌 회원에게 제재할 수 없다")
    void processSanction_MessageReportTargetMismatch_Throws() {
        stubChatMessageReport(Member.builder().id(3L).build());
        ProcessSanctionRequest request = new ProcessSanctionRequest(1L, 2L, SanctionType.WARNING, null, null);

        assertThatThrownBy(() -> reportService.processSanction(request))
                .isInstanceOf(SanctionTargetMismatchException.class);
        verify(sanctionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("메시지 신고에는 게시글 삭제 제재를 쓸 수 없다")
    void processSanction_MessageReportContentDeletion_Throws() {
        stubChatMessageReport(target);
        ProcessSanctionRequest request = new ProcessSanctionRequest(1L, 2L, SanctionType.CONTENT_DELETION, null, null);

        assertThatThrownBy(() -> reportService.processSanction(request))
                .isInstanceOf(UnsupportedSanctionTypeException.class);
        verify(sanctionRepository, never()).saveAndFlush(any());
    }
}
