package com.dongsoop.dongsoop.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.report.dto.ReportListItem;
import com.dongsoop.dongsoop.report.dto.ReportResponse;
import com.dongsoop.dongsoop.report.dto.ReportSummaryResponse;
import com.dongsoop.dongsoop.report.entity.ReportFilterType;
import com.dongsoop.dongsoop.report.entity.ReportReason;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import com.dongsoop.dongsoop.report.service.ReportServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ReportListResponseTest {

    @InjectMocks
    private ReportServiceImpl reportService;
    @Mock
    private ReportRepository reportRepository;

    @ParameterizedTest
    @EnumSource(ReportFilterType.class)
    void preservesFilterSpecificResponseFields(ReportFilterType filter) {
        Pageable pageable = PageRequest.of(0, 20);
        if (filter == ReportFilterType.UNPROCESSED) {
            when(reportRepository.findSummaryReportsByFilter(filter, pageable)).thenReturn(List.of(
                    new ReportSummaryResponse(7L, "reporter", ReportType.MEMBER, ReportReason.OTHER, false, null, 9L, 9L, "description", null, null, null, null, null)));
        } else {
            when(reportRepository.findDetailedReportsByFilter(filter, pageable)).thenReturn(List.of(
                    new ReportResponse(7L, "reporter", ReportType.MEMBER, 9L, null, ReportReason.OTHER, "description", false, null, null, null, null, null, null, null, null, null, null, null, null, null)));
        }

        List<ReportListItem> result = reportService.getReports(filter, pageable);
        JsonNode json = new ObjectMapper().valueToTree(result);

        assertThat(json.size()).isEqualTo(1);
        JsonNode item = json.get(0);
        assertThat(item.get("id").asLong()).isEqualTo(7L);
        assertThat(item.get("reporterNickname").asText()).isEqualTo("reporter");
        assertThat(item.get("reportType").asText()).isEqualTo("MEMBER");
        assertThat(item.get("reportReason").asText()).isEqualTo("OTHER");
        assertThat(item.get("targetId").asLong()).isEqualTo(9L);
        assertThat(item.get("description").asText()).isEqualTo("description");
        assertThat(item.get("isProcessed").asBoolean()).isFalse();
        assertThat(item.has("targetMemberId")).isEqualTo(filter == ReportFilterType.UNPROCESSED);
        assertThat(item.has("sanctionType")).isEqualTo(filter != ReportFilterType.UNPROCESSED);
        assertThat(item.size()).isEqualTo(filter == ReportFilterType.UNPROCESSED ? 14 : 21);
    }
}
