package com.dongsoop.dongsoop.report.util;

import com.dongsoop.dongsoop.report.entity.ReportType;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReportUrlGenerator {

    public String generateUrl(ReportType reportType, Long targetId) {
        String prefix = getUrlPrefixes().get(reportType);
        return prefix + targetId;
    }

    private Map<ReportType, String> getUrlPrefixes() {
        return Map.of(
                ReportType.PROJECT_BOARD, "/api/project-boards/",
                ReportType.STUDY_BOARD, "/api/study-boards/",
                ReportType.MARKETPLACE_BOARD, "/api/marketplace-boards/",
                ReportType.TUTORING_BOARD, "/api/tutoring-boards/",
                ReportType.MEMBER, "/api/members/"
        );
    }
}