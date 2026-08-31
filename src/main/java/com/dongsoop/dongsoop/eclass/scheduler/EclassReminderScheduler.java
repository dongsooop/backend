package com.dongsoop.dongsoop.eclass.scheduler;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.notification.EclassNotification;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 마감이 남은 과제를 D-3 / D-1 / 당일 아침에 알린다. 과제 1건당 알림 1건이라
 * 어느 과목 과제가 며칠 남았는지 제목만 보고 알 수 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EclassReminderScheduler {

    private final EclassAssignmentRepository assignmentRepository;
    private final EclassNotification eclassNotification;
    private final Clock clock;

    @Value("${eclass.reminder.days-before}")
    private List<Integer> daysBefore;

    /**
     * 트랜잭션으로 감싸지 않는다.
     *
     * <p>알림 저장이 같은 트랜잭션에 참여하기 때문에, 발송 하나가 실패하면 트랜잭션이 롤백 전용으로
     * 표시되고 예외를 잡아도 그 표시는 풀리지 않는다. 그러면 이미 보낸 과제의 발송 기록까지 커밋 시점에
     * 사라져 다음 날 같은 알림이 다시 나간다. 조회 결과는 기기까지 함께 가져오므로 지연 로딩도 필요 없다.
     */
    @Scheduled(cron = "${eclass.reminder.cron}", zone = "Asia/Seoul")
    public void remind() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        // 마감이 이미 지난 과제는 제외된다 — 자정 마감 과제의 마지막 알림은 전날 아침이 된다
        LocalDateTime until = today.plusDays(Collections.max(daysBefore))
                .atTime(LocalTime.MAX);
        List<EclassAssignment> targets = assignmentRepository.searchReminderTargets(now, until);

        List<EclassAssignment> sent = new ArrayList<>();
        for (EclassAssignment assignment : targets) {
            int remainingDays = (int) ChronoUnit.DAYS.between(today, assignment.getDueAt().toLocalDate());
            if (!daysBefore.contains(remainingDays) || !assignment.needsReminder(remainingDays)) {
                continue;
            }

            // 실제로 보낸 것만 기록한다 — 기기 토큰이 없거나 알림이 꺼져 건너뛴 과제까지 보냈다고 적으면
            // 그 사용자는 이 단계의 알림을 영영 받지 못한다
            if (!send(assignment, remainingDays)) {
                continue;
            }

            assignment.markReminded(remainingDays);
            sent.add(assignment);
        }

        assignmentRepository.saveAll(sent);
        log.info("eclass reminder ended. targets: {}, sent: {}", targets.size(), sent.size());
    }

    /**
     * 발송 실패 한 건이 그날의 나머지 리마인드를 막지 않게 삼킨다.
     */
    private boolean send(EclassAssignment assignment, int remainingDays) {
        try {
            return eclassNotification.sendReminder(assignment, remainingDays);
        } catch (RuntimeException exception) {
            log.warn("failed to send eclass reminder. assignId: {}", assignment.getAssignId(), exception);
            return false;
        }
    }
}
