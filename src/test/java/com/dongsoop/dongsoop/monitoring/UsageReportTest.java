package com.dongsoop.dongsoop.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.monitoring.client.DiscordWebhookClient;
import com.dongsoop.dongsoop.monitoring.dto.UsageReport;
import com.dongsoop.dongsoop.monitoring.dto.UsageReport.FeatureUsage;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.EndpointCount;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.EndpointLatency;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.FeatureCount;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.HourCount;
import java.time.LocalDateTime;
import com.dongsoop.dongsoop.monitoring.scheduler.UsageReportScheduler;
import com.dongsoop.dongsoop.monitoring.service.UsageReportService;
import com.dongsoop.dongsoop.monitoring.service.UsageReportServiceImpl;
import com.dongsoop.dongsoop.monitoring.util.UsageReportMessage;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UsageReportTest {

    private static final LocalDate FROM = LocalDate.of(2026, 9, 11);
    private static final LocalDate TO = LocalDate.of(2026, 9, 17);

    private static UsageWindow window(long calls, long users, Map<String, FeatureCount> features) {
        return new UsageWindow(calls, users, users * 3 / 4, 420.0, features,
                Map.of(FROM, 312L, FROM.plusDays(1), 280L),
                List.of(new EndpointCount("GET /home", 8_120)),
                List.of(new EndpointLatency("GET /project-board/{boardId}", 1840.0)),
                37, List.of(new EndpointCount("POST /chat/rooms", 31)),
                new HourCount(2, 21, 2_140));
    }

    @Test
    @DisplayName("기능은 사용자 수 순으로 정렬되고 전주 대비 증감을 계산하며 전주 0이면 신규")
    void comparesWindows() {
        UsageWindow current = window(48_213, 1_102, Map.of(
                "home", new FeatureCount(11_405, 790),
                "project-board", new FeatureCount(14_820, 812),
                "eclass", new FeatureCount(612, 40)));
        UsageWindow previous = window(43_000, 1_060, Map.of(
                "home", new FeatureCount(11_000, 770),
                "project-board", new FeatureCount(13_000, 745),
                "marketplace-contact", new FeatureCount(50, 4)));

        UsageReport report = UsageReportServiceImpl.compare(FROM, TO, current, previous);

        assertThat(report.usersDeltaPercent()).isEqualTo(4);
        assertThat(report.callsDeltaPercent()).isEqualTo(12);
        assertThat(report.features()).extracting(FeatureUsage::feature)
                .containsExactly("project-board", "home", "eclass");
        assertThat(report.features().get(0).usersDeltaPercent()).isEqualTo(9);
        assertThat(report.features().get(2).usersDeltaPercent()).isNull();
        assertThat(report.newFeatures()).containsExactly("eclass");
        assertThat(report.droppedFeatures()).containsExactly("marketplace-contact");
        assertThat(report.slowest().get(0).uri()).isEqualTo("GET /project-board/{boardId}");
        assertThat(report.memberUsers()).isEqualTo(826);
    }

    @Test
    @DisplayName("증감률: 감소는 음수, 전주 0은 null")
    void deltaPercent() {
        assertThat(UsageReportServiceImpl.deltaPercent(50, 100)).isEqualTo(-50);
        assertThat(UsageReportServiceImpl.deltaPercent(0, 0)).isZero();
        assertThat(UsageReportServiceImpl.deltaPercent(5, 0)).isNull();
        assertThat(UsageReportServiceImpl.deltaPercent(100, 100)).isZero();
    }

    @Test
    @DisplayName("디스코드 메시지에 기간·순위·증감 표시·눈에 띄는 것이 들어간다")
    void formatsMessage() {
        UsageReport report = new UsageReport(FROM, TO, 1_102, 4, 812, 48_213, 12, 420.0,
                Map.of(FROM, 312L),
                List.of(new FeatureUsage("project-board", 812, 14_820, 9),
                        new FeatureUsage("blinddate", 120, 1_377, 65),
                        new FeatureUsage("eclass", 40, 612, null),
                        new FeatureUsage("system", 1_050, 9_000, 200),
                        new FeatureUsage("meal", 0, 4, 0)),
                List.of("eclass"), List.of("marketplace-contact"),
                List.of(new EndpointCount("GET /home", 8_120)),
                List.of(new EndpointLatency("GET /project-board/{boardId}", 1840.0)),
                37, List.of(new EndpointCount("POST /chat/rooms", 31)),
                new HourCount(2, 21, 2_140));

        String message = UsageReportMessage.format(report, LocalDateTime.of(2026, 9, 18, 9, 0));

        assertThat(message)
                .contains("기간 9/11(금) ~ 9/17(목) · 생성 9/18 09:00")
                .contains("사용자 1,102명 ▲4% · 회원 812 · 비회원 290")
                .contains("API 호출 48,213회 ▲12% · 사용자당 43.8회")
                .contains("5xx 37건 (0.08%) · 응답시간 p95 0.42s")
                .contains("가장 바쁜 시간: 화 21시 (2,140회)")
                .contains("금   312 ▇▇▇▇▇▇▇▇▇▇▇▇")
                .contains("812명 ▲9%")
                .contains("프로젝트 모집")
                .contains("(신규)")
                .doesNotContain("학식")
                .doesNotContain("자동 호출")
                .contains("8,120회  GET /home")
                .contains("1.84s  GET /project-board/{boardId}")
                .contains("31건  POST /chat/rooms")
                .contains("소개팅 사용자 ▲65% (120명)")
                .doesNotContain("프로젝트 모집 사용자 ▲9%")
                .contains("새로 쓰이기 시작: 이클래스")
                .contains("지난주엔 썼는데 이번 주 0명: 장터 문의");
    }

    @Test
    @DisplayName("기록이 없으면 빈 순위와 특이사항 없음")
    void formatsEmptyReport() {
        UsageReport report = UsageReportServiceImpl.compare(FROM, TO, UsageWindow.empty(), UsageWindow.empty());

        String message = UsageReportMessage.format(report);

        assertThat(message).contains("(기록 없음)").contains("특이사항 없음").contains("사용자 0명 ―");
        assertThat(report.newFeatures()).isEmpty();
    }

    @Test
    @DisplayName("2000자를 넘는 메시지는 줄 단위로 나눠 보낸다")
    void splitsLongMessages() {
        String line = "x".repeat(700) + "\n";
        List<String> chunks = DiscordWebhookClient.split(line.repeat(4));

        assertThat(chunks).hasSize(2);
        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(2000));
    }

    @Test
    @DisplayName("웹훅 URL이 없으면 조회조차 하지 않는다")
    void skipsWithoutWebhook() throws IOException {
        UsageReportService service = mock(UsageReportService.class);
        DiscordWebhookClient discord = mock(DiscordWebhookClient.class);

        new UsageReportScheduler(service, discord, "").sendReportEndingAt(TO.plusDays(1));

        verify(service, never()).build(any(), any());
        verify(discord, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("조회가 실패하면 보내지 않고, 성공하면 지난 7일 범위로 보낸다")
    void sendsOnlyOnSuccess() throws IOException {
        UsageReportService service = mock(UsageReportService.class);
        DiscordWebhookClient discord = mock(DiscordWebhookClient.class);
        UsageReportScheduler scheduler = new UsageReportScheduler(service, discord, "https://hook");
        when(service.build(any(), any())).thenThrow(new IOException("es down"));

        scheduler.sendReportEndingAt(TO.plusDays(1));
        verify(discord, never()).send(anyString(), anyString());

        doReturn(UsageReportServiceImpl.compare(FROM, TO, UsageWindow.empty(), UsageWindow.empty()))
                .when(service).build(FROM, TO.plusDays(1));
        scheduler.sendReportEndingAt(TO.plusDays(1));
        verify(discord).send(any(), anyString());
    }
}
