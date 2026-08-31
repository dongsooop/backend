package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.common.crypto.AesGcmEncryptor;
import com.dongsoop.dongsoop.eclass.client.EclassClient;
import com.dongsoop.dongsoop.eclass.client.dto.MoodleAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassAssignment;
import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import com.dongsoop.dongsoop.eclass.exception.EclassApiException;
import com.dongsoop.dongsoop.eclass.exception.EclassInvalidTokenException;
import com.dongsoop.dongsoop.eclass.notification.EclassNotification;
import com.dongsoop.dongsoop.eclass.repository.EclassAssignmentRepository;
import com.dongsoop.dongsoop.eclass.repository.EclassLinkRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    private final EclassLinkRepository linkRepository;
    private final EclassAssignmentRepository assignmentRepository;
    private final EclassClient eclassClient;
    private final AesGcmEncryptor encryptor;
    private final EclassNotification eclassNotification;
    private final Clock clock;

    @Value("${eclass.sync.window-past-days}")
    private int windowPastDays;

    @Value("${eclass.sync.window-future-days}")
    private int windowFutureDays;

    @Value("${eclass.sync.thread-count}")
    private int threadCount;

    @Value("${eclass.sync.request-delay-ms}")
    private long requestDelayMs;

    @Value("${eclass.sync.abort-failure-ratio}")
    private double abortFailureRatio;

    @Value("${eclass.sync.relink-timeout-hours}")
    private long relinkTimeoutHours;

    @Value("${eclass.sync.token-expiry-notice-days}")
    private int tokenExpiryNoticeDays;

    @Override
    public SyncOutcome syncLink(EclassLink link) {
        LocalDateTime now = LocalDateTime.now(clock);
        String token = encryptor.decrypt(link.getTokenEncrypted());

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

        LocalDateTime windowStart = now.minusDays(windowPastDays);
        LocalDateTime windowEnd = now.plusDays(windowFutureDays);

        Map<Long, MoodleAssignment> inWindow = fetched.stream()
                .filter(assignment -> assignment.dueDate() > 0)
                .filter(assignment -> isInWindow(toLocalDateTime(assignment.dueDate()), windowStart, windowEnd))
                .collect(Collectors.toMap(MoodleAssignment::assignId, Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));

        Map<Long, EclassAssignment> existing = assignmentRepository.findAllByLinkId(link.getId()).stream()
                .collect(Collectors.toMap(EclassAssignment::getAssignId, Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));

        List<EclassAssignment> toSave = new ArrayList<>();
        for (MoodleAssignment fetchedAssignment : inWindow.values()) {
            EclassAssignment assignment = merge(link, existing.get(fetchedAssignment.assignId()), fetchedAssignment);

            if (!assignment.isSubmitted()) {
                try {
                    updateSubmission(token, assignment, now);
                } catch (EclassInvalidTokenException exception) {
                    expireLink(link, now);
                    return SyncOutcome.TOKEN_EXPIRED;
                }
            }

            toSave.add(assignment);
        }

        existing.values().stream()
                .filter(assignment -> !inWindow.containsKey(assignment.getAssignId()))
                .filter(assignment -> !assignment.isRemoved())
                .filter(assignment -> isInWindow(assignment.getDueAt(), windowStart, windowEnd))
                .forEach(assignment -> {
                    assignment.markRemoved(now);
                    toSave.add(assignment);
                });

        assignmentRepository.saveAll(toSave);
        link.markSynced(now);
        linkRepository.save(link);

        return SyncOutcome.SYNCED;
    }

    @Override
    public void syncAll() {
        List<EclassLink> links = linkRepository.findAllByStatus(EclassLinkStatus.ACTIVE);
        if (!links.isEmpty()) {
            syncConcurrently(links);
        }

        promoteOverdueRelinks();
        requestPreemptiveRelinks();
    }

    private void syncConcurrently(List<EclassLink> links) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<CompletableFuture<SyncOutcome>> futures = links.stream()
                    .map(link -> CompletableFuture.supplyAsync(() -> syncQuietly(link), executor))
                    .toList();

            long failed = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(SyncOutcome.FAILED::equals)
                    .count();

            double failureRatio = (double) failed / links.size();
            if (failureRatio >= abortFailureRatio) {
                log.error("eclass sync failure ratio too high: {}/{}", failed, links.size());
            }
        } finally {
            shutdown(executor);
        }
    }

    private SyncOutcome syncQuietly(EclassLink link) {
        try {
            return syncLink(link);
        } catch (RuntimeException exception) {
            log.warn("eclass sync failed unexpectedly. linkId: {}", link.getId(), exception);
            return SyncOutcome.FAILED;
        }
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
     * 토큰 만료가 예고된 연동은 만료 전에 미리 재발급을 지시해 수집 공백을 없앤다.
     */
    private void requestPreemptiveRelinks() {
        LocalDateTime now = LocalDateTime.now(clock);

        linkRepository.findAllByStatus(EclassLinkStatus.ACTIVE).stream()
                .filter(link -> link.needsPreemptiveRelink(now, tokenExpiryNoticeDays))
                .forEach(link -> {
                    eclassNotification.sendRelinkSilent(link);
                    link.markRelinkRequested(now);
                    linkRepository.save(link);
                });
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

    private void updateSubmission(String token, EclassAssignment assignment, LocalDateTime now) {
        try {
            if (eclassClient.isSubmitted(token, assignment.getAssignId())) {
                assignment.markSubmitted(now);
            } else {
                assignment.markChecked(now);
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

    private boolean isInWindow(LocalDateTime target, LocalDateTime start, LocalDateTime end) {
        return !target.isBefore(start) && !target.isAfter(end);
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
