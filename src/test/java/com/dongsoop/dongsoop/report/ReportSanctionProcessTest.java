package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.exception.GroupChatOnlyException;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.exception.ReportAlreadyProcessedException;
import com.dongsoop.dongsoop.report.exception.SanctionTargetMismatchException;
import com.dongsoop.dongsoop.report.exception.UnsupportedSanctionTypeException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.ReportServiceImpl;
import com.dongsoop.dongsoop.report.service.SanctionExecutor;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    private SanctionExecutor sanctionExecutor;

    private final Member admin = Member.builder().id(100L).build();
    private final Member target = Member.builder().id(2L).build();
    private Report report;

    @BeforeEach
    void setUp() {
        report = Report.builder().id(1L).reportType(ReportType.MEMBER).targetId(2L).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(report));
        // 이미 처리된 신고는 회원 조회 전에 거절돼 두 스텁이 쓰이지 않는다
        lenient().when(memberRepository.findById(2L)).thenReturn(Optional.of(target));
        lenient().when(memberService.getMemberReferenceByContext()).thenReturn(admin);
    }

    @Test
    @DisplayName("관리자 제재는 신고·관리자·대상·요청 내용을 제재 실행기에 넘긴다")
    void processSanction_DelegatesToIssue() {
        LocalDateTime endAt = LocalDateTime.of(2026, 10, 5, 0, 0);

        reportService.processSanction(new ProcessSanctionRequest(1L, 2L, SanctionType.TEMPORARY_BAN, "욕설", endAt));

        verify(sanctionExecutor).issue(report, admin, target, SanctionType.TEMPORARY_BAN, "욕설", endAt, null);
    }

    @Test
    @DisplayName("이미 처리된 신고에 제재를 요청하면 409로 거절한다")
    void processSanction_AlreadyProcessed_Throws() {
        Report processed = Report.builder().id(1L).reportType(ReportType.MEMBER).targetId(2L).isProcessed(true).build();
        when(reportRepository.findById(1L)).thenReturn(Optional.of(processed));
        ProcessSanctionRequest request = new ProcessSanctionRequest(1L, 2L, SanctionType.WARNING, null, null);

        assertThatThrownBy(() -> reportService.processSanction(request))
                .isInstanceOf(ReportAlreadyProcessedException.class)
                .hasMessage("이미 처리된 신고입니다. ID : 1");
        verify(memberRepository, never()).findById(any());
        verifyNoInteractions(sanctionExecutor);
    }

    @Test
    @DisplayName("채팅 메시지 신고가 아니면 채팅방 추방을 거절한다")
    void processSanction_ChatKickOnNonChatReport_Throws() {
        ProcessSanctionRequest request = new ProcessSanctionRequest(1L, 2L, SanctionType.CHAT_KICK, null, null);

        assertThatThrownBy(() -> reportService.processSanction(request))
                .isInstanceOf(GroupChatOnlyException.class);
        verify(sanctionExecutor, never()).issue(any(), any(), any(), any(), any(), any(), any());
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
        verify(sanctionExecutor, never()).issue(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("메시지 신고에는 게시글 삭제 제재를 쓸 수 없다")
    void processSanction_MessageReportContentDeletion_Throws() {
        stubChatMessageReport(target);
        ProcessSanctionRequest request = new ProcessSanctionRequest(1L, 2L, SanctionType.CONTENT_DELETION, null, null);

        assertThatThrownBy(() -> reportService.processSanction(request))
                .isInstanceOf(UnsupportedSanctionTypeException.class);
        verify(sanctionExecutor, never()).issue(any(), any(), any(), any(), any(), any(), any());
    }
}
