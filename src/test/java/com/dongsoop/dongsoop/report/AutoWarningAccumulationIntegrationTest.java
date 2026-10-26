package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.jwt.service.DeviceBlacklistService;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDevice;
import com.dongsoop.dongsoop.memberdevice.entity.MemberDeviceType;
import com.dongsoop.dongsoop.memberdevice.repository.MemberDeviceRepository;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.Sanction;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.repository.SanctionRepository;
import com.dongsoop.dongsoop.report.service.AsyncAutoSanctionService;
import com.dongsoop.dongsoop.report.service.TextFilteringService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// 자동 판정은 @Async 스레드의 별도 트랜잭션에서 돈다. 테스트 트랜잭션으로 감싸면 그 스레드가 커밋 전 데이터를 못 보므로
// 트랜잭션 없이 커밋하고 끝나면 직접 지운다
@SpringBootTest
@ActiveProfiles("test")
class AutoWarningAccumulationIntegrationTest extends AbstractIntegrationTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private SanctionRepository sanctionRepository;
    @Autowired
    private AsyncAutoSanctionService asyncAutoSanctionService;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MemberDeviceRepository memberDeviceRepository;
    @MockitoBean
    private TextFilteringService textFilteringService;
    @MockitoBean
    private DeviceBlacklistService deviceBlacklistService;
    @Value("${admin.id}")
    private Long systemAdminId;

    private final List<Long> memberIds = new ArrayList<>();

    @BeforeEach
    void insertSystemAdmin() {
        jdbcTemplate.update("""
                INSERT INTO member (id, email, nickname, password, is_deleted)
                VALUES (?, 'system-admin@dongyang.ac.kr', 'system', 'password', false)
                ON CONFLICT (id) DO NOTHING
                """, systemAdminId);
        memberIds.add(systemAdminId);
    }

    @AfterEach
    void cleanUp() {
        String ids = String.join(",", memberIds.stream().map(String::valueOf).toList());
        jdbcTemplate.update("UPDATE report SET sanction_id = NULL WHERE reporter_id IN (" + ids + ")"
                + " OR target_member_id IN (" + ids + ")");
        jdbcTemplate.update("DELETE FROM sanction WHERE member_id IN (" + ids + ")");
        jdbcTemplate.update("DELETE FROM member_device WHERE member_id IN (" + ids + ")");
        jdbcTemplate.update("DELETE FROM report WHERE reporter_id IN (" + ids + ")"
                + " OR target_member_id IN (" + ids + ")");
        jdbcTemplate.update("DELETE FROM member WHERE id IN (" + ids + ")");
        memberIds.clear();
    }

    private Member saveMember(String key) {
        Member member = memberRepository.save(Member.builder()
                .email(key + "@dongyang.ac.kr")
                .nickname(key)
                .password("password")
                .build());
        memberIds.add(member.getId());
        return member;
    }

    private Report saveChatReport(Member reporter, Member target, String messageId) {
        return reportRepository.save(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.CHAT_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.HATE_SPEECH)
                .targetUrl("/chat/room/acc-room")
                .chatRoomId("acc-room")
                .messageId(messageId)
                .messageContent("욕설")
                .build());
    }

    private void saveWarnedChatReport(Member reporter, Member target, Member admin, String messageId) {
        Report report = saveChatReport(reporter, target, messageId);
        LocalDateTime now = LocalDateTime.now(KST);
        Sanction warning = sanctionRepository.save(Sanction.builder()
                .member(target)
                .admin(admin)
                .targetMember(target)
                .report(report)
                .sanctionType(SanctionType.WARNING)
                .reason("욕설")
                .startDate(now)
                .endDate(SanctionType.PERMANENT_END_DATE)
                .build());
        report.processSanction(admin, target, warning);
        reportRepository.save(report);
    }

    @Test
    @DisplayName("경고 2회가 있는 회원의 욕설 채팅 신고를 자동 판정하면 3일 정지가 생긴다")
    void processReportAsync_ThirdAutoWarning_CreatesThreeDayBan() throws Exception {
        Member admin = saveMember("accadm");
        Member reporter = saveMember("accrep");
        Member target = saveMember("acctgt");
        saveWarnedChatReport(reporter, target, admin, "acc-old-1");
        saveWarnedChatReport(reporter, target, admin, "acc-old-2");
        Report fresh = saveChatReport(reporter, target, "acc-new");
        MemberDevice device = memberDeviceRepository.save(MemberDevice.builder()
                .member(target).deviceToken("acc-device").memberDeviceType(MemberDeviceType.ANDROID).build());
        when(textFilteringService.hasProfanity(any(), any(), any())).thenReturn(true);

        asyncAutoSanctionService.processReportAsync(fresh).get(10, TimeUnit.SECONDS);

        List<Sanction> bans = sanctionRepository.findActiveSanctionsByMemberId(target.getId()).stream()
                .filter(sanction -> sanction.getSanctionType() == SanctionType.TEMPORARY_BAN)
                .toList();
        assertThat(bans).hasSize(1);
        assertThat(bans.get(0).getEndDate())
                .isCloseTo(LocalDateTime.now(KST).plusDays(3), within(1, ChronoUnit.MINUTES));
        assertThat(bans.get(0).getReason()).isEqualTo("부적절한 언어 사용");
        assertThat(bans.get(0).getDescription()).isEqualTo("경고 3회 누적으로 인한 자동 3일 정지");
        assertThat(reportRepository.findById(fresh.getId()).orElseThrow().getIsProcessed()).isTrue();
        verify(deviceBlacklistService).blacklist(device.getId());
    }

    private Report saveBlindDateReport(Member reporter, Member target, String messageId) {
        return reportRepository.save(Report.builder()
                .reporter(reporter)
                .reportType(ReportType.BLINDDATE_MESSAGE)
                .targetId(target.getId())
                .targetMember(target)
                .reportReason(ReportReason.SPAM)
                .targetUrl("/blinddate/session/acc-session")
                .chatRoomId("acc-session")
                .messageId(messageId)
                .messageContent("광고")
                .build());
    }

    @Test
    @DisplayName("자동 경고는 같은 메시지의 신고만 함께 닫고, 같은 대상의 다른 메시지·과팅 신고는 관리자에게 남긴다")
    void processReportAsync_AutoWarning_ResolvesSameMessageOnly() throws Exception {
        Member reporter1 = saveMember("smrep1");
        Member reporter2 = saveMember("smrep2");
        Member target = saveMember("smtgt");
        Report fresh = saveChatReport(reporter1, target, "sm-msg");
        Report sameMessage = saveChatReport(reporter2, target, "sm-msg");
        Report otherMessage = saveChatReport(reporter2, target, "sm-other");
        Report blindDate = saveBlindDateReport(reporter1, target, "sm-blind");
        when(textFilteringService.hasProfanity(any(), any(), any())).thenReturn(true);

        asyncAutoSanctionService.processReportAsync(fresh).get(10, TimeUnit.SECONDS);

        assertThat(reportRepository.findById(fresh.getId()).orElseThrow().getSanction()).isNotNull();
        Report resolved = reportRepository.findById(sameMessage.getId()).orElseThrow();
        assertThat(resolved.getIsProcessed()).isTrue();
        assertThat(resolved.getResolvedByReportId()).isEqualTo(fresh.getId());
        for (Report untouched : List.of(otherMessage, blindDate)) {
            Report reloaded = reportRepository.findById(untouched.getId()).orElseThrow();
            assertThat(reloaded.getIsProcessed()).isFalse();
            assertThat(reloaded.getResolvedByReportId()).isNull();
        }
        assertThat(reportRepository.countActiveWarningsForMember(target.getId(), SanctionType.WARNING)).isOne();
    }

    @Test
    @DisplayName("이미 경고된 메시지로 닫힌 신고는 그 경고 신고를 가리켜 목록에서 활성 경고로 보이고, 다른 메시지 신고는 남는다")
    void processReportAsync_AlreadyWarned_PointsToWarningReport() throws Exception {
        Member admin = saveMember("awadm");
        Member reporter1 = saveMember("awrep1");
        Member reporter2 = saveMember("awrep2");
        Member reporter3 = saveMember("awrep3");
        Member target = saveMember("awtgt");
        saveWarnedChatReport(reporter1, target, admin, "aw-msg");
        Long warnedId = reportRepository.findFirstByMessageIdAndSanctionSanctionTypeOrderByIdAsc(
                "aw-msg", SanctionType.WARNING).orElseThrow().getId();
        Report fresh = saveChatReport(reporter2, target, "aw-msg");
        Report sameMessage = saveChatReport(reporter3, target, "aw-msg");
        Report otherMessage = saveChatReport(reporter2, target, "aw-other");
        when(textFilteringService.hasProfanity(any(), any(), any())).thenReturn(true);

        asyncAutoSanctionService.processReportAsync(fresh).get(10, TimeUnit.SECONDS);

        for (Report closed : List.of(fresh, sameMessage)) {
            Report reloaded = reportRepository.findById(closed.getId()).orElseThrow();
            assertThat(reloaded.getIsProcessed()).isTrue();
            assertThat(reloaded.getSanction()).isNull();
            assertThat(reloaded.getResolvedByReportId()).isEqualTo(warnedId);
        }
        assertThat(reportRepository.findById(otherMessage.getId()).orElseThrow().getIsProcessed()).isFalse();
        assertThat(reportRepository.countActiveWarningsForMember(target.getId(), SanctionType.WARNING)).isOne();
        assertThat(reportRepository.findDetailedReportsByFilter(ReportFilterType.ACTIVE_SANCTIONS,
                PageRequest.of(0, 50)))
                .filteredOn(report -> report.id().equals(fresh.getId()))
                .singleElement()
                .satisfies(report -> {
                    assertThat(report.sanctionType()).isEqualTo(SanctionType.WARNING);
                    assertThat(report.isSanctionActive()).isTrue();
                });
    }

    @Test
    @DisplayName("필터 호출이 실패한 채팅 신고는 자동 판정 완료로 표시돼 다음 스케줄러 조회에서 빠진다")
    void processReportAsync_FilterFailure_MarksAutoReviewed() throws Exception {
        Member reporter = saveMember("flrrep");
        Member target = saveMember("flrtgt");
        Report report = saveChatReport(reporter, target, "flr-msg");
        // 실제 TextFilteringService는 호출 실패·타임아웃 시 false를 돌려준다
        when(textFilteringService.hasProfanity(any(), any(), any())).thenReturn(false);

        asyncAutoSanctionService.processReportAsync(report).get(10, TimeUnit.SECONDS);

        Report reloaded = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(reloaded.getIsAutoReviewed()).isTrue();
        assertThat(reloaded.getIsProcessed()).isFalse();
        assertThat(reportRepository.findUnprocessedReports(PageRequest.of(0, 50)))
                .extracting(Report::getId)
                .doesNotContain(report.getId());
    }

    @Test
    @DisplayName("필터를 기다리는 동안 관리자가 기각하면 욕설로 판정돼도 자동 경고를 만들지 않는다")
    void processReportAsync_AdminDismissesDuringFiltering_NoAutoWarning() throws Exception {
        Member admin = saveMember("dsmadm");
        Member reporter = saveMember("dsmrep");
        Member target = saveMember("dsmtgt");
        Report report = saveChatReport(reporter, target, "dsm-msg");
        when(textFilteringService.hasProfanity(any(), any(), any())).thenAnswer(invocation -> {
            jdbcTemplate.update("UPDATE report SET is_processed = true, admin_id = ? WHERE id = ?",
                    admin.getId(), report.getId());
            return true;
        });

        asyncAutoSanctionService.processReportAsync(report).get(10, TimeUnit.SECONDS);

        Report reloaded = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(reloaded.getSanction()).isNull();
        assertThat(sanctionRepository.findActiveSanctionsByMemberId(target.getId())).isEmpty();
    }
}
