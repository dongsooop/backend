package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.blinddate.entity.BlindDateMessage;
import com.dongsoop.dongsoop.blinddate.event.BlindDateSessionClosedEvent;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportChatRoomType;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.exception.ReportAlreadyProcessedException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.service.BlindDateContextAfterRecorder;
import com.dongsoop.dongsoop.report.service.ReportService;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private SanctionRepository sanctionRepository;
    @Autowired
    private ReportService reportService;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private MemberService memberService;

    private Member saveMember(String key) {
        return memberRepository.save(Member.builder()
                .email(key + "@dongyang.ac.kr")
                .nickname(key)
                .password("password")
                .build());
    }

    private Report saveReport(Member reporter, Member target, ReportType type) {
        return reportRepository.save(Report.builder()
                .reporter(reporter)
                .reportType(type)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.OTHER)
                .targetUrl("/test/" + target.getId())
                .build());
    }

    @Test
    @DisplayName("자동제재 스케줄러 조회는 게시판 신고만 가져온다")
    void findUnprocessedReports_ReturnsBoardReportsOnly() {
        Member reporter = saveMember("rep1");
        Member target = saveMember("tgt1");
        Report boardReport = saveReport(reporter, target, ReportType.PROJECT_BOARD);
        Report memberReport = saveReport(reporter, target, ReportType.MEMBER);
        entityManager.flush();

        List<Long> ids = reportRepository.findUnprocessedReports(PageRequest.of(0, 50)).stream()
                .map(Report::getId)
                .toList();

        assertThat(ids).contains(boardReport.getId()).doesNotContain(memberReport.getId());
    }

    @Test
    @DisplayName("관리자 영구정지는 실제 DB 제약을 통과해 저장된다")
    void processSanction_PersistsWithDatabaseConstraints() {
        Member admin = saveMember("adm1");
        Member reporter = saveMember("rep2");
        Member target = saveMember("tgt2");
        Report report = saveReport(reporter, target, ReportType.MEMBER);
        when(memberService.getMemberReferenceByContext()).thenReturn(admin);

        reportService.processSanction(new ProcessSanctionRequest(
                report.getId(), target.getId(), SanctionType.PERMANENT_BAN, null, null));
        entityManager.flush();
        entityManager.clear();

        Sanction saved = sanctionRepository.findActiveSanctionsByMemberId(target.getId()).get(0);
        assertThat(saved.getEndDate()).isEqualTo(SanctionType.PERMANENT_END_DATE);
        assertThat(saved.getReport().getId()).isEqualTo(report.getId());
    }

    @Test
    @Sql(scripts = "classpath:migration/417_chat_report.sql")
    @DisplayName("채팅 신고 마이그레이션이 Postgres에서 실행되고 채팅 신고가 저장된다")
    void chatReportMigration_RunsAndChatReportPersists() {
        Member reporter = saveMember("rep3");
        Member target = saveMember("tgt3");
        reportRepository.save(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.CHAT_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.HATE_SPEECH)
                .targetUrl("/chat/room/room1")
                .chatRoomId("room1")
                .messageId("msg1")
                .messageContent("욕설")
                .build());
        entityManager.flush();

        Integer checkConstraints = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'report_report_type_check'", Integer.class);
        assertThat(checkConstraints).isZero();
        Integer contextAfterColumns = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns"
                        + " WHERE table_name = 'report' AND column_name = 'message_context_after'", Integer.class);
        assertThat(contextAfterColumns).isOne();
    }

    @Test
    @Sql(scripts = {"classpath:migration/417_chat_report.sql", "classpath:migration/417_blinddate_report.sql"})
    @DisplayName("뒤쪽 맥락이 없는 과팅 신고만 세션 ID로 찾고, 저장한 뒤쪽 맥락은 다시 읽힌다")
    void blindDateContextAfter_FindsOnlyMissingAndPersists() {
        Member reporter = saveMember("rep10");
        Member target = saveMember("tgt10");
        Member other = saveMember("oth10");
        Report missing = reportRepository.save(blindDateReport(reporter, target, "s10", "m10"));
        Report filled = reportRepository.save(blindDateReport(reporter, other, "s10", "m11"));
        filled.recordContextAfter(new ChatMessageSnapshots(List.of(ChatMessageSnapshot.of(other.getId(), "뒤", null))));
        reportRepository.save(blindDateReport(target, reporter, "other-session", "m12"));
        entityManager.flush();
        entityManager.clear();

        assertThat(reportRepository.findByReportTypeAndChatRoomIdAndMessageContextAfterIsNull(
                ReportType.BLINDDATE_MESSAGE, "s10")).extracting(Report::getId).containsExactly(missing.getId());
        assertThat(reportRepository.findById(filled.getId()).orElseThrow().getMessageContextAfter().messages())
                .extracting(ChatMessageSnapshot::content).containsExactly("뒤");
    }

    @Test
    @Sql(scripts = {"classpath:migration/417_chat_report.sql", "classpath:migration/417_blinddate_report.sql"})
    @DisplayName("뒤쪽 맥락 저장은 그 컬럼만 갱신해, 먼저 읽힌 뒤 다른 처리가 바꾼 처리 상태를 덮어쓰지 않는다")
    void blindDateContextAfter_PartialUpdateKeepsProcessedState() {
        Member admin = saveMember("cxadm");
        Member reporter = saveMember("cxrep");
        Member target = saveMember("cxtgt");
        Report report = reportRepository.save(blindDateReport(reporter, target, "s20", "m1"));
        entityManager.flush();
        entityManager.clear();
        // 레코더가 읽은 뒤 관리자 처리가 커밋된 상황: 영속성 컨텍스트에는 미처리 상태가 남아 있다
        assertThat(reportRepository.findById(report.getId()).orElseThrow().getIsProcessed()).isFalse();
        jdbcTemplate.update("UPDATE report SET is_processed = true, admin_id = ? WHERE id = ?", admin.getId(),
                report.getId());
        LocalDateTime sentAt = LocalDateTime.of(2026, 9, 28, 21, 0);
        List<BlindDateMessage> messages = List.of(
                new BlindDateMessage("m1", target.getId(), "신고된 말", sentAt),
                new BlindDateMessage("m2", reporter.getId(), "뒤 대화", sentAt.plusSeconds(1)));

        new BlindDateContextAfterRecorder(reportRepository)
                .onSessionClosed(new BlindDateSessionClosedEvent("s20", messages));
        entityManager.flush();
        entityManager.clear();

        Report reloaded = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(reloaded.getIsProcessed()).isTrue();
        assertThat(reloaded.getAdmin().getId()).isEqualTo(admin.getId());
        assertThat(reloaded.getMessageContextAfter().messages())
                .containsExactly(new ChatMessageSnapshot(reporter.getId(), "뒤 대화", sentAt.plusSeconds(1)));
        assertThat(reportRepository.recordContextAfter(report.getId(), new ChatMessageSnapshots(List.of())))
                .isZero();
    }

    private static Report blindDateReport(Member reporter, Member target, String sessionId, String messageId) {
        return Report.builder()
                .reporter(reporter)
                .reportType(ReportType.BLINDDATE_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.INAPPROPRIATE_CONTENT)
                .targetUrl("/blinddate/session/" + sessionId)
                .chatRoomId(sessionId)
                .messageId(messageId)
                .messageContent("무례한 말")
                .build();
    }

    @Test
    @DisplayName("자동 판정이 끝난 채팅 신고는 스케줄러가 다시 가져가지 않는다")
    void findUnprocessedReports_SkipsAutoReviewedChatReports() {
        Member reporter = saveMember("rep4");
        Member target = saveMember("tgt4");
        Report fresh = saveReport(reporter, target, ReportType.CHAT_MESSAGE);
        Report reviewed = saveReport(reporter, target, ReportType.CHAT_MESSAGE);
        reviewed.markAutoReviewed();
        entityManager.flush();

        List<Long> ids = reportRepository.findUnprocessedReports(PageRequest.of(0, 50)).stream()
                .map(Report::getId)
                .toList();

        assertThat(ids).contains(fresh.getId()).doesNotContain(reviewed.getId());
    }

    @Test
    @DisplayName("자동제재 스케줄러 조회는 과팅 신고를 가져가지 않는다")
    void findUnprocessedReports_SkipsBlindDateReports() {
        Member reporter = saveMember("rep9");
        Member target = saveMember("tgt9");
        saveReport(reporter, target, ReportType.BLINDDATE_MESSAGE);
        entityManager.flush();

        List<Long> ids = reportRepository.findUnprocessedReports(PageRequest.of(0, 50)).stream()
                .map(Report::getId)
                .toList();

        assertThat(ids).isEmpty();
    }

    @Test
    @DisplayName("관리자 목록은 채팅 신고의 메시지와 맥락을 함께 돌려준다")
    void adminList_IncludesChatFields() {
        Member reporter = saveMember("rep5");
        Member target = saveMember("tgt5");
        reportRepository.save(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.CHAT_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.HATE_SPEECH)
                .targetUrl("/chat/room/room5")
                .chatRoomId("room5")
                .messageId("msg5")
                .messageContent("욕설")
                .messageContext(new ChatMessageSnapshots(List.of(ChatMessageSnapshot.of(target.getId(), "앞말", null))))
                .build());
        entityManager.flush();
        entityManager.clear();

        var detailed = reportRepository.findDetailedReportsByFilter(ReportFilterType.ALL, PageRequest.of(0, 50));
        var summary = reportRepository.findSummaryReportsByFilter(ReportFilterType.UNPROCESSED, PageRequest.of(0, 50));

        assertThat(detailed).anySatisfy(report -> {
            assertThat(report.messageId()).isEqualTo("msg5");
            assertThat(report.messageContent()).isEqualTo("욕설");
            assertThat(report.messageContext().messages()).hasSize(1);
        });
        assertThat(summary).anySatisfy(report -> {
            assertThat(report.chatRoomId()).isEqualTo("room5");
            assertThat(report.messageContext().messages()).extracting(ChatMessageSnapshot::content)
                    .containsExactly("앞말");
        });
    }

    @Test
    @Sql(scripts = {"classpath:migration/417_chat_report.sql", "classpath:migration/417_blinddate_report.sql"})
    @DisplayName("과팅 신고 마이그레이션이 Postgres에서 실행되고 과팅 신고가 저장된다")
    void blindDateReportMigration_RunsAndReportPersists() {
        Member reporter = saveMember("rep6");
        Member target = saveMember("tgt6");
        reportRepository.save(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.BLINDDATE_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.INAPPROPRIATE_CONTENT)
                .targetUrl("/blinddate/session/s6")
                .chatRoomId("s6")
                .messageId("m6")
                .messageContent("무례한 말")
                .build());
        entityManager.flush();

        assertThat(reportRepository.existsByReporterIdAndReportTypeAndChatRoomIdAndTargetMemberId(
                reporter.getId(), ReportType.BLINDDATE_MESSAGE, "s6", target.getId())).isTrue();
    }

    @Test
    @Sql(scripts = {"classpath:migration/417_chat_report.sql", "classpath:migration/417_blinddate_report.sql"})
    @DisplayName("같은 메시지를 두 번 신고하면 채팅 신고 유니크 인덱스가 실제 Postgres에서 막는다")
    void chatReportUniqueIndex_BlocksDuplicateInPostgres() {
        Member reporter = saveMember("rep7");
        Member target = saveMember("tgt7");
        reportRepository.saveAndFlush(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.CHAT_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.HATE_SPEECH)
                .targetUrl("/chat/room/room7")
                .chatRoomId("room7")
                .messageId("dup-msg")
                .messageContent("욕설")
                .build());

        Report duplicate = Report.builder()
                .reporter(reporter)
                .reportType(ReportType.CHAT_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.HATE_SPEECH)
                .targetUrl("/chat/room/room7")
                .chatRoomId("room7")
                .messageId("dup-msg")
                .messageContent("욕설")
                .build();

        assertThatThrownBy(() -> reportRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(e -> assertThat(constraintNameOf((DataIntegrityViolationException) e))
                        .isEqualToIgnoringCase("uk_report_reporter_message"));
    }

    @Test
    @Sql(scripts = {"classpath:migration/417_chat_report.sql", "classpath:migration/417_blinddate_report.sql"})
    @DisplayName("같은 세션·같은 상대의 과팅 신고는 유니크 인덱스가 막지만, 채팅 타입은 걸리지 않는다")
    void blindDateReportUniqueIndex_BlocksDuplicateButNotChatType() {
        Member reporter = saveMember("rep8");
        Member target = saveMember("tgt8");

        // 부분 인덱스(WHERE report_type = 'BLINDDATE_MESSAGE')라 같은 세션·같은 상대라도 CHAT_MESSAGE는 걸리지 않아야 한다
        reportRepository.saveAndFlush(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.CHAT_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.HATE_SPEECH)
                .targetUrl("/chat/room/s8")
                .chatRoomId("s8")
                .messageId("chat-msg-1")
                .messageContent("욕설")
                .build());
        reportRepository.saveAndFlush(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.CHAT_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.HATE_SPEECH)
                .targetUrl("/chat/room/s8")
                .chatRoomId("s8")
                .messageId("chat-msg-2")
                .messageContent("욕설")
                .build());

        reportRepository.saveAndFlush(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.BLINDDATE_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.INAPPROPRIATE_CONTENT)
                .targetUrl("/blinddate/session/s8")
                .chatRoomId("s8")
                .messageId("blind-msg-1")
                .messageContent("무례한 말")
                .build());

        Report duplicateBlindDate = Report.builder()
                .reporter(reporter)
                .reportType(ReportType.BLINDDATE_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.INAPPROPRIATE_CONTENT)
                .targetUrl("/blinddate/session/s8")
                .chatRoomId("s8")
                .messageId("blind-msg-2")
                .messageContent("무례한 말")
                .build();

        assertThatThrownBy(() -> reportRepository.saveAndFlush(duplicateBlindDate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(e -> assertThat(constraintNameOf((DataIntegrityViolationException) e))
                        .isEqualToIgnoringCase("uk_report_blinddate_reporter_target"));
    }

    private Report saveMessageReport(Member reporter, Member target, String roomId, String messageId) {
        return reportRepository.save(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.CHAT_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.HATE_SPEECH)
                .targetUrl("/chat/room/" + roomId)
                .chatRoomId(roomId)
                .chatRoomType(ReportChatRoomType.GROUP)
                .messageId(messageId)
                .messageContent("욕설")
                .build());
    }

    private Report saveBlindDateReport(Member reporter, Member target, String sessionId, String messageId) {
        return reportRepository.save(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.BLINDDATE_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.INAPPROPRIATE_CONTENT)
                .targetUrl("/blinddate/session/" + sessionId)
                .chatRoomId(sessionId)
                .chatRoomType(ReportChatRoomType.BLINDDATE)
                .messageId(messageId)
                .messageContent("무례한 말")
                .build());
    }

    @Test
    @Sql(scripts = {"classpath:migration/417_chat_report.sql", "classpath:migration/417_blinddate_report.sql",
            "classpath:migration/648_report_followups.sql"})
    @DisplayName("메시지 신고를 제재하면 같은 대상의 미처리 메시지 신고가 함께 닫히고, 목록에는 처리한 신고의 활성 제재로 보인다")
    void processSanction_ResolvesSameTargetReportsAndShowsResolverSanction() {
        Member admin = saveMember("rsadm");
        Member target = saveMember("rstgt");
        Member otherTarget = saveMember("rsoth");
        Member reporter1 = saveMember("rsrep1");
        Member reporter2 = saveMember("rsrep2");
        Member reporter3 = saveMember("rsrep3");
        Report processing = saveMessageReport(reporter1, target, "rs-room", "rs-msg");
        Report otherMessage = saveMessageReport(reporter2, target, "rs-room2", "rs-other");
        Report blindDate = saveBlindDateReport(reporter3, target, "rs-session", "rs-blind");
        Report otherTargetReport = saveMessageReport(reporter2, otherTarget, "rs-room", "rs-msg2");
        Report board = saveReport(reporter2, target, ReportType.PROJECT_BOARD);
        entityManager.flush();
        when(memberService.getMemberReferenceByContext()).thenReturn(admin);

        reportService.processSanction(new ProcessSanctionRequest(
                processing.getId(), target.getId(), SanctionType.WARNING, "욕설", null));
        entityManager.flush();
        entityManager.clear();

        for (Report sibling : List.of(otherMessage, blindDate)) {
            Report resolved = reportRepository.findById(sibling.getId()).orElseThrow();
            assertThat(resolved.getIsProcessed()).isTrue();
            assertThat(resolved.getAdmin().getId()).isEqualTo(admin.getId());
            assertThat(resolved.getResolvedByReportId()).isEqualTo(processing.getId());
            assertThat(resolved.getSanction()).isNull();
        }
        assertThat(reportRepository.findById(otherTargetReport.getId()).orElseThrow().getIsProcessed()).isFalse();
        assertThat(reportRepository.findById(board.getId()).orElseThrow().getIsProcessed()).isFalse();
        assertThat(reportRepository.countActiveWarningsForMember(target.getId(), SanctionType.WARNING)).isOne();
        assertThat(sanctionRepository.findActiveSanctionsByMemberId(target.getId())).hasSize(1);
        for (ReportFilterType filter : List.of(ReportFilterType.PROCESSED, ReportFilterType.ACTIVE_SANCTIONS)) {
            assertThat(reportRepository.findDetailedReportsByFilter(filter, PageRequest.of(0, 50)))
                    .filteredOn(report -> report.id().equals(blindDate.getId()))
                    .singleElement()
                    .satisfies(report -> {
                        assertThat(report.resolvedByReportId()).isEqualTo(processing.getId());
                        assertThat(report.chatRoomType()).isEqualTo(ReportChatRoomType.BLINDDATE);
                        assertThat(report.sanctionType()).isEqualTo(SanctionType.WARNING);
                        assertThat(report.sanctionReason()).isEqualTo("욕설");
                        assertThat(report.sanctionStartDate()).isNotNull();
                        assertThat(report.sanctionEndDate()).isEqualTo(SanctionType.PERMANENT_END_DATE);
                        assertThat(report.isSanctionActive()).isTrue();
                        assertThat(report.adminNickname()).isEqualTo("rsadm");
                    });
        }
        assertThat(reportRepository.findSummaryReportsByFilter(ReportFilterType.UNPROCESSED, PageRequest.of(0, 50)))
                .extracting(report -> report.id())
                .doesNotContain(otherMessage.getId(), blindDate.getId())
                .contains(otherTargetReport.getId());
        Integer chatRoomTypeChecks = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'report_chat_room_type_check'", Integer.class);
        assertThat(chatRoomTypeChecks).isZero();
    }

    @Test
    @DisplayName("기각으로 함께 닫힌 신고는 목록에서 제재 없음으로 보이고 활성 제재 목록에 없다")
    void dismissReport_ResolvedSiblingShowsNoSanction() {
        Member admin = saveMember("dsadm");
        Member target = saveMember("dstgt");
        Member reporter1 = saveMember("dsrep1");
        Member reporter2 = saveMember("dsrep2");
        Report dismissed = saveMessageReport(reporter1, target, "ds-room", "ds-msg");
        Report sibling = saveMessageReport(reporter2, target, "ds-room", "ds-other");
        entityManager.flush();
        when(memberService.getMemberReferenceByContext()).thenReturn(admin);

        reportService.dismissReport(dismissed.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(reportRepository.findDetailedReportsByFilter(ReportFilterType.PROCESSED, PageRequest.of(0, 50)))
                .filteredOn(report -> report.id().equals(sibling.getId()))
                .singleElement()
                .satisfies(report -> {
                    assertThat(report.resolvedByReportId()).isEqualTo(dismissed.getId());
                    assertThat(report.sanctionType()).isNull();
                    assertThat(report.isSanctionActive()).isNull();
                    assertThat(report.adminNickname()).isEqualTo("dsadm");
                });
        assertThat(reportRepository.findDetailedReportsByFilter(ReportFilterType.ACTIVE_SANCTIONS,
                PageRequest.of(0, 50))).extracting(report -> report.id()).doesNotContain(sibling.getId());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("같은 대상의 다른 신고를 동시에 제재하면 늦은 쪽은 대상 잠금을 기다린 뒤 409로 끝나고 교착 없이 제재는 한 건이다")
    void processSanction_ConcurrentSameTarget_SerializesWithoutDeadlock() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Member admin = tx.execute(status -> saveMember("lkadm"));
        Member target = tx.execute(status -> saveMember("lktgt"));
        Member reporter1 = tx.execute(status -> saveMember("lkrep1"));
        Member reporter2 = tx.execute(status -> saveMember("lkrep2"));
        Report first = tx.execute(status -> saveMessageReport(reporter1, target, "lk-room", "lk-msg"));
        Report second = tx.execute(status -> saveMessageReport(reporter2, target, "lk-room", "lk-other"));
        when(memberService.getMemberReferenceByContext()).thenReturn(admin);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        // 먼저 잠근 처리가 늦은 처리의 시작 뒤에 함께 해결을 하게 해, 신고 행만 잠그면 서로의 행을 기다려 교착되는 순서를 만든다
        Thread holder = new Thread(() -> tx.executeWithoutResult(status -> {
            reportRepository.findByIdForUpdateLockingTarget(first.getId());
            firstLocked.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            reportService.processSanction(new ProcessSanctionRequest(
                    first.getId(), target.getId(), SanctionType.WARNING, "욕설", null));
        }));
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            holder.start();
            assertThat(firstLocked.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> late = executor.submit(() -> reportService.processSanction(new ProcessSanctionRequest(
                    second.getId(), target.getId(), SanctionType.WARNING, "욕설", null)));

            assertThatThrownBy(() -> late.get(500, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            holder.join(10_000);

            assertThatThrownBy(() -> late.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(ReportAlreadyProcessedException.class);
            Report resolved = reportRepository.findById(second.getId()).orElseThrow();
            assertThat(resolved.getResolvedByReportId()).isEqualTo(first.getId());
            assertThat(reportRepository.countActiveWarningsForMember(target.getId(), SanctionType.WARNING)).isOne();
            assertThat(sanctionRepository.findActiveSanctionsByMemberId(target.getId())).hasSize(1);
        } finally {
            release.countDown();
            holder.join(10_000);
            executor.shutdownNow();
            jdbcTemplate.update("UPDATE report SET sanction_id = NULL WHERE id IN (?, ?)", first.getId(),
                    second.getId());
            jdbcTemplate.update("DELETE FROM sanction WHERE member_id = ?", target.getId());
            jdbcTemplate.update("DELETE FROM report WHERE id IN (?, ?)", first.getId(), second.getId());
            jdbcTemplate.update("DELETE FROM member WHERE id IN (?, ?, ?, ?)", admin.getId(), target.getId(),
                    reporter1.getId(), reporter2.getId());
        }
    }

    // 예외 발생 뒤 Postgres 트랜잭션이 abort 상태가 되어 추가 조회가 실패하므로, 예외 객체에서만 제약 이름을 꺼낸다
    private static String constraintNameOf(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                return constraintViolation.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }
}
