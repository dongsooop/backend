package com.dongsoop.dongsoop.eclass.scheduler;

import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.notification.EclassNotification;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Scheduled(cron = "${eclass.reminder.cron}", zone = "Asia/Seoul")
    @Transactional
    public void remind() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();

        // 마감이 이미 지난 과제는 제외된다 — 자정 마감 과제의 마지막 알림은 전날 아침이 된다
        LocalDateTime until = today.plusDays(Collections.max(daysBefore))
                .atTime(LocalTime.MAX);
        List<EclassAssignment> targets = assignmentRepository.searchReminderTargets(now, until);

        int sent = 0;
        for (EclassAssignment assignment : targets) {
            int remainingDays = (int) ChronoUnit.DAYS.between(today, assignment.getDueAt().toLocalDate());
            if (!daysBefore.contains(remainingDays) || !assignment.needsReminder(remainingDays)) {
                continue;
            }

            if (!send(assignment, remainingDays)) {
                continue;
            }

            assignment.markReminded(remainingDays);
            sent++;
        }

        assignmentRepository.saveAll(targets);
        log.info("eclass reminder ended. targets: {}, sent: {}", targets.size(), sent);
    }

    /**
     * 발송 실패 한 건이 그날 리마인드 전체를 되돌리지 않게 삼킨다.
     *
     * <p>여기서 예외가 올라가면 트랜잭션이 말리면서 이미 보낸 과제의 발송 기록까지 사라져,
     * 다음 날 같은 알림이 다시 나간다.
     */
    private boolean send(EclassAssignment assignment, int remainingDays) {
        try {
            eclassNotification.sendReminder(assignment, remainingDays);
            return true;
        } catch (RuntimeException exception) {
            log.warn("failed to send eclass reminder. assignId: {}", assignment.getAssignId(), exception);
            return false;
        }
    }
}
