package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.report.dto.ProcessSanctionRequest;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.service.ReportService;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    @Sql(scripts = "classpath:migration/chat_report.sql")
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
}
