package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.report.dto.ReportContextResponse;
import com.dongsoop.dongsoop.report.dto.ReportContextResponse.AfterSource;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.exception.ReportNotFoundException;
import com.dongsoop.dongsoop.report.exception.UnsupportedReportTypeException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.BlindDateReportTargetResolver;
import com.dongsoop.dongsoop.report.service.ChatReportTargetResolver;
import com.dongsoop.dongsoop.report.service.ReportServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportContextServiceTest {

    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 9, 28, 21, 0);
    private static final ChatMessageSnapshots BEFORE = snapshots("앞말");
    private static final ChatMessageSnapshots AFTER = snapshots("뒷말");

    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ChatReportTargetResolver chatReportTargetResolver;
    @Mock
    private BlindDateReportTargetResolver blindDateReportTargetResolver;

    private static ChatMessageSnapshots snapshots(String content) {
        return new ChatMessageSnapshots(List.of(ChatMessageSnapshot.of(2L, content, SENT_AT)));
    }

    private void givenReport(ReportType type, ChatMessageSnapshots contextAfter) {
        when(reportRepository.findById(1L)).thenReturn(Optional.of(Report.builder()
                .id(1L).reportType(type).chatRoomId("room").messageId("msg").messageContent("욕설")
                .messageContext(BEFORE).messageContextAfter(contextAfter).build()));
    }

    private static void assertContext(ReportContextResponse response, List<String> after, AfterSource source) {
        assertThat(response.messageId()).isEqualTo("msg");
        assertThat(response.messageContent()).isEqualTo("욕설");
        assertThat(response.before()).extracting(ChatMessageSnapshot::content).containsExactly("앞말");
        assertThat(response.after()).extracting(ChatMessageSnapshot::content).containsExactlyElementsOf(after);
        assertThat(response.afterSource()).isEqualTo(source);
    }

    @Test
    @DisplayName("채팅 신고는 원문에서 뒤쪽 맥락을 가져와 LIVE로 응답한다")
    void chat_Live() {
        givenReport(ReportType.CHAT_MESSAGE, null);
        when(chatReportTargetResolver.findContextAfter("room", "msg")).thenReturn(Optional.of(AFTER));

        assertContext(reportService.getReportContext(1L), List.of("뒷말"), AfterSource.LIVE);
    }

    @Test
    @DisplayName("채팅 원문에서 신고 메시지를 못 찾으면 빈 목록과 UNAVAILABLE이다")
    void chat_Unavailable() {
        givenReport(ReportType.CHAT_MESSAGE, null);
        when(chatReportTargetResolver.findContextAfter("room", "msg")).thenReturn(Optional.empty());

        assertContext(reportService.getReportContext(1L), List.of(), AfterSource.UNAVAILABLE);
    }

    @Test
    @DisplayName("과팅 신고에 저장된 뒤쪽 맥락이 있으면 세션을 보지 않고 SAVED로 응답한다")
    void blindDate_Saved() {
        givenReport(ReportType.BLINDDATE_MESSAGE, AFTER);

        assertContext(reportService.getReportContext(1L), List.of("뒷말"), AfterSource.SAVED);
        verifyNoInteractions(blindDateReportTargetResolver);
    }

    @Test
    @DisplayName("저장본이 없고 세션이 살아 있으면 세션 메모리에서 LIVE로 응답한다")
    void blindDate_Live() {
        givenReport(ReportType.BLINDDATE_MESSAGE, null);
        when(blindDateReportTargetResolver.findLiveContextAfter("room", "msg")).thenReturn(Optional.of(AFTER));

        assertContext(reportService.getReportContext(1L), List.of("뒷말"), AfterSource.LIVE);
    }

    @Test
    @DisplayName("저장본도 세션도 없으면 UNAVAILABLE이다")
    void blindDate_Unavailable() {
        givenReport(ReportType.BLINDDATE_MESSAGE, null);
        when(blindDateReportTargetResolver.findLiveContextAfter("room", "msg")).thenReturn(Optional.empty());

        assertContext(reportService.getReportContext(1L), List.of(), AfterSource.UNAVAILABLE);
    }

    @Test
    @DisplayName("게시판 신고는 메시지 신고가 아니라 400으로 거절한다")
    void boardReport_Rejected() {
        givenReport(ReportType.PROJECT_BOARD, null);

        assertThatThrownBy(() -> reportService.getReportContext(1L))
                .isInstanceOf(UnsupportedReportTypeException.class);
    }

    @Test
    @DisplayName("없는 신고는 404다")
    void missingReport_NotFound() {
        when(reportRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getReportContext(1L))
                .isInstanceOf(ReportNotFoundException.class);
    }
}
