package com.dongsoop.dongsoop.eclass.service;

import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentResponse;
import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentSaveRequest;
import java.util.List;

public interface EclassAssignmentService {

    List<EclassAssignmentResponse> saveAssignments(Long memberId, EclassAssignmentSaveRequest request);

    List<EclassAssignmentResponse> getAssignments(Long memberId);
}
