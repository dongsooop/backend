package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentListResponse;
import com.dongsoop.dongsoop.home.dto.HomeEclassSummary;

public interface EclassAssignmentService {

    EclassAssignmentListResponse getUpcoming(String fid, String deviceToken);

    HomeEclassSummary getHomeSummary(Long memberId, String fid, String deviceToken);

    HomeEclassSummary getHomeSummary(String fid, String deviceToken);
}
