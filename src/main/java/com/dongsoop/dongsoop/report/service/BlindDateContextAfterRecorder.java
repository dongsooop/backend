package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.blinddate.event.BlindDateSessionClosedEvent;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 과팅 메시지 기록은 세션이 지워지면 사라지므로, 그 직전에 받은 기록으로 과팅 신고의 뒤쪽 맥락을 채운다.
 */
@Component
@RequiredArgsConstructor
public class BlindDateContextAfterRecorder {

    private final ReportRepository reportRepository;

    // 예외는 과팅 스레드로 돌아가지 않고 Spring 기본 비동기 예외 처리기가 로그로 남긴다
    @Async
    @EventListener
    @Transactional
    public void onSessionClosed(BlindDateSessionClosedEvent event) {
        for (Report report : reportRepository.findByReportTypeAndChatRoomIdAndMessageContextAfterIsNull(
                ReportType.BLINDDATE_MESSAGE, event.sessionId())) {
            report.recordContextAfter(
                    BlindDateReportTargetResolver.contextAfter(event.messages(), report.getMessageId()));
        }
    }
}
