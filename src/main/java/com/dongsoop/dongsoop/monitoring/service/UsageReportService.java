package com.dongsoop.dongsoop.monitoring.service;

import com.dongsoop.dongsoop.monitoring.dto.UsageReport;
import java.io.IOException;
import java.time.LocalDate;

public interface UsageReportService {

    /** [from, to) 기간과 그 직전 같은 길이 기간을 비교한 리포트 */
    UsageReport build(LocalDate from, LocalDate to) throws IOException;
}
