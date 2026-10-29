package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;

class BlindDateContextAfterRecorderTest {

  private static final String SESSION_ID = "s1";
  private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 9, 28, 21, 0);

  private final ReportRepository reportRepository = mock(ReportRepository.class);
  private final BlindDateContextAfterRecorder recorder =
      new BlindDateContextAfterRecorder(reportRepository);

  private static Report report(String messageId) {
    return Report.builder()
        .id(1L)
        .reportType(ReportType.BLINDDATE_MESSAGE)
        .chatRoomId(SESSION_ID)
        .messageId(messageId)
        .build();
  }

  private static List<BlindDateMessage> messages(int count) {
    return IntStream.range(0, count)
        .mapToObj(i -> new BlindDateMessage("m" + i, (long) i, "내용" + i, SENT_AT.plusSeconds(i)))
        .toList();
  }

  private ChatMessageSnapshots closeAndCaptureRecorded(Report report) {
    when(reportRepository.findByReportTypeAndChatRoomIdAndMessageContextAfterIsNull(
            ReportType.BLINDDATE_MESSAGE, SESSION_ID))
        .thenReturn(List.of(report));
    recorder.onSessionClosed(new BlindDateSessionClosedEvent(SESSION_ID, messages(15)));
    ArgumentCaptor<ChatMessageSnapshots> recorded =
        ArgumentCaptor.forClass(ChatMessageSnapshots.class);
    verify(reportRepository).recordContextAfter(eq(report.getId()), recorded.capture());
    return recorded.getValue();
  }

  @Test
  @DisplayName("신고된 메시지 다음 메시지만 오래된 순으로 최대 10개를 부분 갱신으로 저장한다")
  void onSessionClosed_SavesNextTenInOrder() {
    ChatMessageSnapshots recorded = closeAndCaptureRecorded(report("m2"));

    assertThat(recorded.messages())
        .extracting(ChatMessageSnapshot::content)
        .containsExactly("내용3", "내용4", "내용5", "내용6", "내용7", "내용8", "내용9", "내용10", "내용11", "내용12");
    assertThat(recorded.messages().get(0))
        .isEqualTo(new ChatMessageSnapshot(3L, "내용3", SENT_AT.plusSeconds(3)));
  }

  @Test
  @DisplayName("신고된 메시지가 기록에 없으면 빈 목록을 저장한다")
  void onSessionClosed_MissingMessage_SavesEmpty() {
    assertThat(closeAndCaptureRecorded(report("gone")).messages()).isEmpty();
  }
}
