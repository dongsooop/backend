package com.dongsoop.dongsoop.monitoring.util;

import com.dongsoop.dongsoop.monitoring.constant.FeatureLabel;
import com.dongsoop.dongsoop.monitoring.dto.UsageReport;
import com.dongsoop.dongsoop.monitoring.dto.UsageReport.FeatureUsage;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class UsageReportMessage {

    static final int TOP_FEATURES = 10;
    static final int NOTABLE_DELTA_PERCENT = 30;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("M/d");

    private UsageReportMessage() {
    }

    public static String format(UsageReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 동숲 주간 사용 리포트 (")
                .append(DATE.format(report.from())).append(" ~ ").append(DATE.format(report.to())).append(")\n");
        sb.append("사용자 ").append(number(report.users())).append("명 ").append(delta(report.usersDeltaPercent()))
                .append(" · API 호출 ").append(number(report.calls())).append("회 ").append(delta(report.callsDeltaPercent()))
                .append("\n\n");

        sb.append("기능별 사용자 수\n");
        List<FeatureUsage> top = report.features().stream().limit(TOP_FEATURES).toList();
        if (top.isEmpty()) {
            sb.append(" (기록 없음)\n");
        }
        for (int i = 0; i < top.size(); i++) {
            FeatureUsage f = top.get(i);
            sb.append(String.format("%d. %s %s명 %s (%s회)%n",
                    i + 1, FeatureLabel.of(f.feature()), number(f.users()), delta(f.usersDeltaPercent()),
                    number(f.calls())));
        }

        sb.append("\n이번 주 눈에 띄는 것\n");
        int notable = 0;
        for (FeatureUsage f : report.features()) {
            Integer d = f.usersDeltaPercent();
            if (d != null && Math.abs(d) >= NOTABLE_DELTA_PERCENT) {
                sb.append(" • ").append(FeatureLabel.of(f.feature())).append(" 사용자 ").append(delta(d)).append("\n");
                notable++;
            }
        }
        if (report.slowest() != null) {
            sb.append(String.format(" • 가장 느린 API: %s p95 %.1fs%n", report.slowest().uri(), report.slowest().p95Ms() / 1000.0));
            notable++;
        }
        if (report.serverErrors() > 0) {
            sb.append(" • 5xx ").append(number(report.serverErrors())).append("건");
            if (report.topErrorUri() != null) {
                sb.append(", 최다: ").append(report.topErrorUri());
            }
            sb.append("\n");
            notable++;
        }
        if (notable == 0) {
            sb.append(" • 특이사항 없음\n");
        }
        return sb.toString().stripTrailing();
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

    static String number(long value) {
        return String.format("%,d", value);
    }
}
