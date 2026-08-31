package com.dongsoop.dongsoop.eclass.dto;

import java.util.List;

public record EclassAssignmentListResponse(

        boolean linked,
        List<EclassAssignmentResponse> assignments
) {

    public static EclassAssignmentListResponse unlinked() {
        return new EclassAssignmentListResponse(false, List.of());
    }
}
