package com.dongsoop.dongsoop.report.util;

import com.dongsoop.dongsoop.report.entity.ReportType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class ReportUrlGenerator {

    private final Map<ReportType, String> urlPrefixes;

    public ReportUrlGenerator() {
        this.urlPrefixes = new EnumMap<>(ReportType.class);
        initializeUrlPrefixes();
    }

    public String generateUrl(ReportType reportType, Long targetId) {
        String prefix = getUrlPrefix(reportType);
        return prefix + targetId;
    }

    private String getUrlPrefix(ReportType reportType) {
        String prefix = urlPrefixes.get(reportType);

        if (prefix == null) {
            throw new IllegalArgumentException("Undefined ReportType: " + reportType);
        }

        return prefix;
    }

    private void initializeUrlPrefixes() {
        urlPrefixes.put(ReportType.PROJECT_BOARD, "/project-board/");
        urlPrefixes.put(ReportType.STUDY_BOARD, "/study-board/");
        urlPrefixes.put(ReportType.MARKETPLACE_BOARD, "/marketplace-board/");
        urlPrefixes.put(ReportType.TUTORING_BOARD, "/tutoring-board/");
        urlPrefixes.put(ReportType.MEMBER, "/member/");
    }
}