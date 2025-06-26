package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.handler.ContentDeletionHandler;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SanctionExecutor {

    private static final long WARNING_THRESHOLD = 3L;

    private final ReportRepository reportRepository;
    private final ContentDeletionHandler contentDeletionHandler;

    @Transactional
    public void executeSanction(Report report) {
        getSanctionExecutors()
                .get(report.getSanctionType())
                .accept(report);
    }

    private Map<SanctionType, Consumer<Report>> getSanctionExecutors() {
        return Map.of(
                SanctionType.WARNING, this::executeWarning,
                SanctionType.TEMPORARY_BAN, this::executeTemporaryBan,
                SanctionType.PERMANENT_BAN, this::executePermanentBan,
                SanctionType.CONTENT_DELETION, this::executeContentDeletion
        );
    }

    private void executeWarning(Report report) {
        log.info("경고 제재 실행: {}", report.getId());
        checkWarningAccumulation(report.getTargetMember());
    }

    private void executeTemporaryBan(Report report) {
        log.info("일시정지 제재 실행: {}", report.getId());
    }

    private void executePermanentBan(Report report) {
        log.info("영구정지 제재 실행: {}", report.getId());
    }

    private void executeContentDeletion(Report report) {
        contentDeletionHandler.deleteContent(report);
        log.info("게시글 삭제 제재 실행: {}", report.getId());
    }

    private void checkWarningAccumulation(Member member) {
        Long warningCount = reportRepository.countActiveWarningsForMember(member.getId());
        logAutoSuspensionWhen(warningCount, member);
    }

    private void logAutoSuspensionWhen(Long warningCount, Member member) {
        Optional.of(warningCount)
                .filter(count -> count >= WARNING_THRESHOLD)
                .ifPresent(count -> {
                    log.info("경고 {}회 누적으로 인한 자동 일시정지: {}", WARNING_THRESHOLD, member.getId());
                });
    }
}