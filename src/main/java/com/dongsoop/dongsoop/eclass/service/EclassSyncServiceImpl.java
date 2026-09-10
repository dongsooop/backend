package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.eclass.dto.MoodleAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import com.dongsoop.dongsoop.eclass.exception.EclassApiException;
import com.dongsoop.dongsoop.eclass.exception.EclassInvalidTokenException;
import com.dongsoop.dongsoop.eclass.notification.EclassNotification;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.eclass.repository.EclassLinkRepository;
import com.dongsoop.dongsoop.eclass.util.EclassClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 이클래스 과제 수집.
 *
 * <p>Moodle 3.2에는 "다가오는 이벤트" API가 없어서 과제 전체를 받아 마감일로 거른다.
 * 수강 과목에 종료일이 비어 있어 과목으로는 옛 학기를 걸러낼 수 없기 때문이기도 하다.
 *
 * <p>외부 호출이 길어 트랜잭션으로 감싸지 않는다 — 저장만 짧게 끊어서 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EclassSyncServiceImpl implements EclassSyncService {

    private static final int MIN_ABORT_SAMPLE = 5;

    private final EclassLinkRepository linkRepository;
    private final EclassAssignmentRepository assignmentRepository;
    private final EclassClient eclassClient;
    private final TextEncryptor eclassTokenEncryptor;
    private final EclassNotification eclassNotification;
    private final Clock clock;

    @Value("${eclass.sync.window-past-days}")
    private int windowPastDays;

    @Value("${eclass.sync.window-future-days}")
    private int windowFutureDays;

    @Value("${eclass.sync.submission-check-days}")
    private int submissionCheckDays;

    @Value("${eclass.sync.thread-count}")
    private int threadCount;

    @Value("${eclass.sync.request-delay-ms}")
    private long requestDelayMs;

    @Value("${eclass.sync.abort-failure-ratio}")
    private double abortFailureRatio;

    @Value("${eclass.sync.relink-timeout-hours}")
    private long relinkTimeoutHours;

    @Override
    public SyncOutcome syncLink(EclassLink link) {
        LocalDateTime now = LocalDateTime.now(clock);
        String token = eclassTokenEncryptor.decrypt(link.getTokenEncrypted());

        List<MoodleAssignment> fetched;
        try {
            fetched = eclassClient.getAssignments(token);
        } catch (EclassInvalidTokenException exception) {
            expireLink(link, now);
            return SyncOutcome.TOKEN_EXPIRED;
        } catch (EclassApiException exception) {
            log.warn("eclass sync failed. linkId: {}, reason: {}", link.getId(), exception.getMessage());
            return SyncOutcome.FAILED;
        }

        Map<Long, MoodleAssignment> inWindow = selectInWindow(fetched, now);
        Map<Long, EclassAssignment> existing = assignmentRepository.findAllByLinkId(link.getId()).stream()
                .collect(Collectors.toMap(EclassAssignment::getAssignId, Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));

        List<EclassAssignment> toSave = new ArrayList<>();
        List<EclassAssignment> dueDateAdvanced = new ArrayList<>();
        try {
            for (MoodleAssignment fetchedAssignment : inWindow.values()) {
                EclassAssignment existingAssignment = existing.get(fetchedAssignment.assignId());
                LocalDateTime previousDueAt = existingAssignment == null ? null : existingAssignment.getDueAt();
                EclassAssignment assignment = merge(link, existingAssignment, fetchedAssignment);
                toSave.add(assignment);

                if (isDueDateAdvanced(previousDueAt, assignment)) {
                    dueDateAdvanced.add(assignment);
                }
                if (needsSubmissionCheck(assignment, now)) {
                    updateSubmission(token, assignment);
                }
            }
        } catch (EclassInvalidTokenException exception) {
            // 여기까지 모은 변경은 버리지 않는다 — 재연동 전까지 되찾을 방법이 없다
            assignmentRepository.saveAll(toSave);
            expireLink(link, now);
            return SyncOutcome.TOKEN_EXPIRED;
        }

        toSave.addAll(markRemoved(existing.values(), inWindow, now));

        // 연동 직후 첫 수집과 정기 수집이 겹치면 같은 과제를 양쪽에서 새로 만들어
        // (eclass_link_id, assign_id) 유니크 제약에 걸린다. 먼저 끝난 쪽이 이미 저장했으므로
        // 이번 회차만 실패로 접고 다음 주기에 맞춘다. 수집 도중 연동이 해제되면 이미 지워진 행을
        // 갱신하려다 낙관적 잠금 예외가 나는데, 되살릴 것이 없으므로 같은 방식으로 접는다
        try {
            assignmentRepository.saveAll(toSave);
            link.markSynced(now);
            linkRepository.save(link);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException exception) {
            log.warn("eclass sync collided with a concurrent run or an unlink. linkId: {}", link.getId());
            return SyncOutcome.FAILED;
        }

        // 저장이 끝난 뒤에 알린다 — 발송이 실패해도 수집 결과는 남는다
        dueDateAdvanced.stream()
                // 이번 회차의 제출 조회에서 제출로 바뀐 과제는 알릴 이유가 없다
                .filter(assignment -> !assignment.isSubmitted())
                .forEach(assignment -> notifyDueDateAdvanced(link, assignment));

        return SyncOutcome.SYNCED;
    }

    @Override
    @Scheduled(cron = "${eclass.sync.cron}", zone = "Asia/Seoul")
    public void syncAll() {
        log.info("eclass sync scheduler started");
        syncConcurrently(linkRepository.findAllByStatus(EclassLinkStatus.ACTIVE));
        promoteOverdueRelinks();
        log.info("eclass sync scheduler ended");
    }

    private void syncConcurrently(List<EclassLink> links) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        // 이클래스가 막히거나 죽은 상태에서 남은 요청을 끝까지 쏘면 차단이 길어진다.
        // 실패가 임계치를 넘으면 그 주기를 그대로 접고 다음 주기를 기다린다.
        AtomicBoolean aborted = new AtomicBoolean(false);
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger attempted = new AtomicInteger();

        try {
            List<CompletableFuture<Void>> futures = links.stream()
                    .map(link -> CompletableFuture.runAsync(
                            () -> syncQuietly(link, links.size(), aborted, failed, attempted), executor))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .join();

            if (aborted.get()) {
                log.error("eclass sync aborted. failed: {}/{} attempted, total links: {}",
                        failed.get(), attempted.get(), links.size());
            }
        } finally {
            shutdown(executor);
        }
    }

    private void syncQuietly(EclassLink link, int totalLinks, AtomicBoolean aborted,
                             AtomicInteger failed, AtomicInteger attempted) {
        if (aborted.get()) {
            return;
        }

        SyncOutcome outcome;
        try {
            outcome = syncLink(link);
        } catch (RuntimeException exception) {
            log.warn("eclass sync failed unexpectedly. linkId: {}", link.getId(), exception);
            outcome = SyncOutcome.FAILED;
        }

        int attemptedCount = attempted.incrementAndGet();
        if (outcome != SyncOutcome.FAILED) {
            return;
        }

        int failedCount = failed.incrementAndGet();
        if (shouldAbort(failedCount, attemptedCount, totalLinks)) {
            aborted.set(true);
        }
    }

    /**
     * 표본이 너무 작을 때 한두 건의 실패로 주기를 접지 않도록, 최소 시도 수를 넘긴 뒤부터 비율을 본다.
     */
    private boolean shouldAbort(int failedCount, int attemptedCount, int totalLinks) {
        int minimumSample = Math.min(MIN_ABORT_SAMPLE, totalLinks);

        return attemptedCount >= minimumSample
                && (double) failedCount / attemptedCount >= abortFailureRatio;
    }

    /**
     * 무음 재발급 지시를 보낸 지 오래도록 재연동이 없으면(앱을 안 열었거나 비밀번호가 바뀐 경우)
     * 그때서야 사용자에게 보이는 알림으로 승격한다.
     */
    private void promoteOverdueRelinks() {
        LocalDateTime now = LocalDateTime.now(clock);
        Duration timeout = Duration.ofHours(relinkTimeoutHours);

        linkRepository.findAllByStatus(EclassLinkStatus.EXPIRED).stream()
                .filter(link -> link.isRelinkOverdue(now, timeout))
                .forEach(link -> {
                    eclassNotification.sendExpiredNotice(link);
                    link.markExpiredNotified(now);
                    linkRepository.save(link);
                });
    }

    /**
     * 마감이 없는 과제(duedate=0)와 수집 창 밖의 과제는 걸러낸다.
     */
    private Map<Long, MoodleAssignment> selectInWindow(List<MoodleAssignment> fetched, LocalDateTime now) {
        return fetched.stream()
                .filter(assignment -> assignment.dueDate() > 0)
                .filter(assignment -> isInWindow(toLocalDateTime(assignment.dueDate()), now))
                .collect(Collectors.toMap(MoodleAssignment::assignId, Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));
    }

    /**
     * 창 안에 있는데 이번 응답에서 사라진 과제는 교수가 지운 것으로 보고 삭제 표시한다.
     */
    private List<EclassAssignment> markRemoved(Collection<EclassAssignment> existing,
                                               Map<Long, MoodleAssignment> inWindow, LocalDateTime now) {
        List<EclassAssignment> removed = existing.stream()
                .filter(assignment -> !inWindow.containsKey(assignment.getAssignId()))
                .filter(assignment -> !assignment.isRemoved())
                .filter(assignment -> isInWindow(assignment.getDueAt(), now))
                .toList();
        removed.forEach(assignment -> assignment.markRemoved(now));

        return removed;
    }

    private EclassAssignment merge(EclassLink link, EclassAssignment existing, MoodleAssignment fetched) {
        LocalDateTime dueAt = toLocalDateTime(fetched.dueDate());
        LocalDateTime cutoffAt = fetched.cutoffDate() > 0 ? toLocalDateTime(fetched.cutoffDate()) : null;

        if (existing == null) {
            return new EclassAssignment(link, fetched.assignId(), fetched.courseModuleId(), fetched.courseName(),
                    fetched.name(), dueAt, cutoffAt);
        }

        existing.update(fetched.courseName(), fetched.name(), dueAt, cutoffAt);
        return existing;
    }

    /**
     * 제출 여부는 리마인드가 나가는 기간에 든 과제만 확인한다.
     *
     * <p>마감이 3주 남은 과제의 제출 여부는 지금 알아도 쓸 데가 없는 반면, 과제 1건마다 이클래스 호출이
     * 1회씩 늘어난다. 확인 범위를 좁히는 것이 학교 서버로 나가는 요청을 줄이는 가장 큰 수단이다.
     */
    private boolean needsSubmissionCheck(EclassAssignment assignment, LocalDateTime now) {
        return !assignment.isSubmitted()
                && !assignment.getDueAt().isAfter(now.plusDays(submissionCheckDays));
    }

    /**
     * 마감이 앞당겨졌는지 판단한다.
     *
     * <p>미뤄진 마감은 리마인드 단계가 초기화돼 새 일정으로 다시 알림이 나가므로 따로 알릴 필요가 없다.
     * 반대로 앞당겨진 마감은 사용자가 알던 날짜보다 급해졌고, 이미 지나버린 경우에는 리마인드 대상에서
     * 아예 빠지기 때문에 여기서 알리지 않으면 사용자가 끝까지 모른 채 지나간다.
     */
    private boolean isDueDateAdvanced(LocalDateTime previousDueAt, EclassAssignment merged) {
        return previousDueAt != null
                && merged.getDueAt().isBefore(previousDueAt);
    }

    private void notifyDueDateAdvanced(EclassLink link, EclassAssignment assignment) {
        try {
            eclassNotification.sendDueDateChanged(link, assignment);
        } catch (RuntimeException exception) {
            log.warn("failed to send due date change notice. assignId: {}", assignment.getAssignId(), exception);
        }
    }

    private void updateSubmission(String token, EclassAssignment assignment) {
        try {
            if (eclassClient.isSubmitted(token, assignment.getAssignId())) {
                assignment.markSubmitted();
            }
        } catch (EclassApiException exception) {
            log.warn("submission status check failed. assignId: {}", assignment.getAssignId());
        }

        sleepQuietly();
    }

    private void expireLink(EclassLink link, LocalDateTime now) {
        boolean wasActive = link.isActive();
        link.expire(now);
        linkRepository.save(link);

        if (wasActive) {
            eclassNotification.sendRelinkSilent(link);
        }
    }

    // 창은 날짜 단위다 — 30일째 되는 날 밤 마감 과제가 오전 수집에서 빠지지 않아야 한다
    private boolean isInWindow(LocalDateTime target, LocalDateTime now) {
        LocalDate date = target.toLocalDate();
        LocalDate today = now.toLocalDate();

        return !date.isBefore(today.minusDays(windowPastDays)) && !date.isAfter(today.plusDays(windowFutureDays));
    }

    private LocalDateTime toLocalDateTime(long epochSecond) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), clock.getZone());
    }

    private void sleepQuietly() {
        if (requestDelayMs <= 0) {
            return;
        }

        try {
            Thread.sleep(requestDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void shutdown(ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
