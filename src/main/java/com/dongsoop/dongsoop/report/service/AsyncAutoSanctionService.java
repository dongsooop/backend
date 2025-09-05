package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.handler.ContentDeletionHandler;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AsyncAutoSanctionService {

    private static final String AUTO_SANCTION_REASON = "부적절한 언어 사용";
    private static final String AUTO_SANCTION_DESCRIPTION = "자동 제재에 의한 게시글 삭제";
    private final SanctionRepository sanctionRepository;
    private final ContentDeletionHandler contentDeletionHandler;
    private final BoardContentService boardContentService;
    private final TextFilteringService textFilteringService;
    private final ReportRepository reportRepository;
    @Value("${admin.id}")
    private Long SYSTEM_ADMIN_ID;

    @Async
    public CompletableFuture<Void> processReportAsync(Report report) {
        try {
            log.info("신고 처리 시작 - Report ID: {}", report.getId());

            validateReportType(report);

            checkProfanityAndExecute(report);

            log.info("자동 제재 완료 - Report ID: {}", report.getId());

        } catch (Exception e) {
            log.error("자동 제재 실패 - Report ID: {}", report.getId(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    private void validateReportType(Report report) {
        if (ReportType.MEMBER.equals(report.getReportType())) {
            log.info("멤버 신고는 자동 처리 제외 - Report ID: {}", report.getId());
            throw new IllegalStateException("멤버 신고는 자동 처리 대상이 아닙니다");
        }
    }

    private void checkProfanityAndExecute(Report report) {
        String title = boardContentService.getTitle(report.getTargetId(), report.getReportType());
        String content = boardContentService.getContent(report.getTargetId(), report.getReportType());

        boolean hasProfanity = textFilteringService.hasProfanity(title, "", content);

        log.info("욕설 필터링 결과 - Report ID: {}, HasProfanity: {}", report.getId(), hasProfanity);

        if (!hasProfanity) {
            log.info("욕설 없음 - Report ID: {}", report.getId());
            throw new IllegalStateException("욕설이 발견되지 않음");
        }

        executeSanction(report);
    }

    private void executeSanction(Report report) {
        Member systemAdmin = createSystemAdmin();
        Sanction sanction = createSanction(report, systemAdmin);

        sanctionRepository.save(sanction);
        report.processSanction(systemAdmin, report.getTargetMember(), sanction);
        reportRepository.save(report);
        contentDeletionHandler.deleteContent(report);
    }

    private Member createSystemAdmin() {
        return Member.builder()
                .id(SYSTEM_ADMIN_ID)
                .build();
    }

    private Sanction createSanction(Report report, Member systemAdmin) {
        return Sanction.builder()
                .member(report.getTargetMember())
                .admin(systemAdmin)
                .report(report)
                .sanctionType(SanctionType.CONTENT_DELETION)
                .reason(AUTO_SANCTION_REASON)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .description(AUTO_SANCTION_DESCRIPTION)
                .build();
    }
}