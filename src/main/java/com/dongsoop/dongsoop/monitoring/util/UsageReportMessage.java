package com.dongsoop.dongsoop.monitoring.util;

import com.dongsoop.dongsoop.monitoring.constant.FeatureLabel;
import com.dongsoop.dongsoop.monitoring.dto.UsageReport;
import com.dongsoop.dongsoop.monitoring.dto.UsageReport.FeatureUsage;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.EndpointCount;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.EndpointLatency;
import com.dongsoop.dongsoop.monitoring.interceptor.ApiUsageInterceptor;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** 주간 리포트를 디스코드 마크다운 본문으로 만든다. 표는 코드 블록, 한글 라벨은 줄 끝에 둬 정렬이 깨지지 않게 한다. */
public final class UsageReportMessage {

    static final int TOP_FEATURES = 10;
    static final int TOP_SLOWEST = 3;
    static final int NOTABLE_DELTA_PERCENT = 30;
    private static final int BAR_WIDTH = 12;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("M/d HH:mm");
    private static final String[] DAY_KO = {"", "월", "화", "수", "목", "금", "토", "일"};
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private UsageReportMessage() {
    }

    public static String format(UsageReport report) {
        return format(report, LocalDateTime.now(ZONE));
    }

    public static String format(UsageReport r, LocalDateTime generatedAt) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 **동숲 주간 사용 리포트**\n");
        sb.append("기간 ").append(day(r.from())).append(" ~ ").append(day(r.to()))
                .append(" · 생성 ").append(DATE_TIME.format(generatedAt)).append("\n\n");

        sb.append("**요약**\n");
        sb.append("• 사용자 ").append(n(r.users())).append("명 ").append(delta(r.usersDeltaPercent()))
                .append(" · 회원 ").append(n(r.memberUsers())).append(" · 비회원 ").append(n(r.users() - r.memberUsers())).append("\n");
        sb.append("• API 호출 ").append(n(r.calls())).append("회 ").append(delta(r.callsDeltaPercent()));
        if (r.users() > 0) {
            sb.append(String.format(" · 사용자당 %.1f회", (double) r.calls() / r.users()));
        }
        sb.append("\n");
        sb.append("• 5xx ").append(n(r.serverErrors())).append("건");
        if (r.calls() > 0) {
            sb.append(String.format(" (%.2f%%)", r.serverErrors() * 100.0 / r.calls()));
        }
        sb.append(String.format(" · 응답시간 p95 %.2fs%n", r.p95Ms() / 1000.0));
        if (r.peakHour() != null) {
            sb.append("• 가장 바쁜 시간: ").append(DAY_KO[r.peakHour().dayOfWeek()]).append(' ')
                    .append(r.peakHour().hour()).append("시 (").append(n(r.peakHour().count())).append("회)\n");
        }

        if (!r.dailyUsers().isEmpty()) {
            sb.append("\n**일별 사용자**\n```\n");
            long max = r.dailyUsers().values().stream().mapToLong(Long::longValue).max().orElse(0);
            for (Map.Entry<LocalDate, Long> e : r.dailyUsers().entrySet()) {
                sb.append(String.format("%s %5s %s%n", DAY_KO[e.getKey().getDayOfWeek().getValue()], n(e.getValue()), bar(e.getValue(), max)));
            }
            sb.append("```\n");
        }

        sb.append("\n**기능별 사용자 (상위 ").append(TOP_FEATURES).append(")**\n");
        List<FeatureUsage> top = r.features().stream()
                .filter(f -> f.users() > 0 && !ApiUsageInterceptor.SYSTEM_FEATURE.equals(f.feature()))
                .limit(TOP_FEATURES).toList();
        if (top.isEmpty()) {
            sb.append("(기록 없음)\n");
        } else {
            sb.append("```\n");
            for (int i = 0; i < top.size(); i++) {
                FeatureUsage f = top.get(i);
                sb.append(String.format("%2d %6s명 %-7s %8s회  %s%n", i + 1, n(f.users()), delta(f.usersDeltaPercent()),
                        n(f.calls()), FeatureLabel.of(f.feature())));
            }
            sb.append("```\n");
        }

        if (!r.topByCalls().isEmpty()) {
            sb.append("\n**많이 부른 API TOP ").append(r.topByCalls().size()).append("**\n```\n");
            for (EndpointCount e : r.topByCalls()) {
                sb.append(String.format("%8s회  %s%n", n(e.count()), e.uri()));
            }
            sb.append("```\n");
        }
        List<EndpointLatency> slow = r.slowest().stream().limit(TOP_SLOWEST).toList();
        if (!slow.isEmpty()) {
            sb.append("\n**느린 API TOP ").append(slow.size()).append("** (p95)\n```\n");
            for (EndpointLatency e : slow) {
                sb.append(String.format("%6.2fs  %s%n", e.p95Ms() / 1000.0, e.uri()));
            }
            sb.append("```\n");
        }
        if (!r.errorsByUri().isEmpty()) {
            sb.append("\n**에러 많은 API TOP ").append(r.errorsByUri().size()).append("** (5xx)\n```\n");
            for (EndpointCount e : r.errorsByUri()) {
                sb.append(String.format("%6s건  %s%n", n(e.count()), e.uri()));
            }
            sb.append("```\n");
        }

        sb.append("\n**눈에 띄는 것**\n");
        int notable = 0;
        for (FeatureUsage f : r.features()) {
            Integer d = f.usersDeltaPercent();
            if (ApiUsageInterceptor.SYSTEM_FEATURE.equals(f.feature())) {
                continue;
            }
            if (d != null && Math.abs(d) >= NOTABLE_DELTA_PERCENT) {
                sb.append("• ").append(FeatureLabel.of(f.feature())).append(" 사용자 ").append(delta(d))
                        .append(" (").append(n(f.users())).append("명)\n");
                notable++;
            }
        }
        if (!r.newFeatures().isEmpty()) {
            sb.append("• 새로 쓰이기 시작: ").append(labels(r.newFeatures())).append("\n");
            notable++;
        }
        if (!r.droppedFeatures().isEmpty()) {
            sb.append("• 지난주엔 썼는데 이번 주 0명: ").append(labels(r.droppedFeatures())).append("\n");
            notable++;
        }
        if (notable == 0) {
            sb.append("• 특이사항 없음\n");
        }
        return sb.toString().stripTrailing();
    }

    private static String day(LocalDate date) {
        return DATE.format(date) + "(" + DAY_KO[date.getDayOfWeek().getValue()] + ")";
    }

    private static String labels(List<String> features) {
        return String.join(", ", features.stream().map(FeatureLabel::of).toList());
    }

    private static String bar(long value, long max) {
        if (max <= 0) {
            return "";
        }
        int width = (int) Math.round((double) value * BAR_WIDTH / max);
        return "▇".repeat(Math.max(width, value > 0 ? 1 : 0));
    }

    static String delta(Integer percent) {
        if (percent == null) {
            return "(신규)";
        }
        if (percent > 0) {
            return "▲" + percent + "%";
        }
        if (percent < 0) {
            return "▼" + (-percent) + "%";
        }
        return "―";
    }

    static String n(long value) {
        return String.format("%,d", value);
    }
}
