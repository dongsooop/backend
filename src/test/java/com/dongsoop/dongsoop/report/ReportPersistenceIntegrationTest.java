package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshot;
import com.dongsoop.dongsoop.report.entity.ChatMessageSnapshots;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.service.ReportService;
import jakarta.persistence.EntityManager;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

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
        Report blindDateReport = saveReport(reporter, target, ReportType.BLINDDATE_MESSAGE);
        entityManager.flush();

        List<Long> ids = reportRepository.findUnprocessedReports(PageRequest.of(0, 50)).stream()
                .map(Report::getId)
                .toList();

        assertThat(ids).doesNotContain(blindDateReport.getId());
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
            assertThat(report.messageContext().messages()).extracting(snapshot -> snapshot.content())
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
