package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.event.BlindDateSessionClosedEvent;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.BlindDateContextAfterRecorder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlindDateContextAfterRecorderTest {

    private static final String SESSION_ID = "s1";
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 9, 28, 21, 0);

    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final BlindDateContextAfterRecorder recorder = new BlindDateContextAfterRecorder(reportRepository);

    private static Report report(String messageId) {
        return Report.builder().reportType(ReportType.BLINDDATE_MESSAGE).chatRoomId(SESSION_ID).messageId(messageId)
                .build();
    }

    private static List<BlindDateMessage> messages(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> new BlindDateMessage("m" + i, (long) i, "내용" + i, SENT_AT.plusSeconds(i)))
                .toList();
    }

    private void close(Report... reports) {
        when(reportRepository.findByReportTypeAndChatRoomIdAndMessageContextAfterIsNull(
                ReportType.BLINDDATE_MESSAGE, SESSION_ID)).thenReturn(List.of(reports));
        recorder.onSessionClosed(new BlindDateSessionClosedEvent(SESSION_ID, messages(15)));
    }

    @Test
    @DisplayName("신고된 메시지 다음 메시지만 오래된 순으로 최대 10개 저장한다")
    void onSessionClosed_SavesNextTenInOrder() {
        Report report = report("m2");

        close(report);

        assertThat(report.getMessageContextAfter().messages())
                .extracting(ChatMessageSnapshot::content)
                .containsExactly("내용3", "내용4", "내용5", "내용6", "내용7", "내용8", "내용9", "내용10", "내용11", "내용12");
        assertThat(report.getMessageContextAfter().messages().get(0))
                .isEqualTo(new ChatMessageSnapshot(3L, "내용3", SENT_AT.plusSeconds(3)));
    }

    @Test
    @DisplayName("신고된 메시지가 기록에 없으면 빈 목록을 저장한다")
    void onSessionClosed_MissingMessage_SavesEmpty() {
        Report report = report("gone");

        close(report);

        assertThat(report.getMessageContextAfter().messages()).isEmpty();
    }

    @Test
    @DisplayName("이미 뒤쪽 맥락이 있는 신고는 덮어쓰지 않는다")
    void onSessionClosed_AlreadyFilled_Skips() {
        Report report = report("m2");
        ChatMessageSnapshots saved = new ChatMessageSnapshots(List.of(ChatMessageSnapshot.of(9L, "먼저 저장", SENT_AT)));
        report.recordContextAfter(saved);

        close(report);

        assertThat(report.getMessageContextAfter()).isSameAs(saved);
    }
}
