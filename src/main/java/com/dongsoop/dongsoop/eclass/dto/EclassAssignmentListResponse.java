package com.dongsoop.dongsoop.eclass.dto;

import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import java.util.List;

public record EclassAssignmentListResponse(

        boolean linked,
        EclassLinkStatus status,
        List<EclassAssignmentResponse> assignments
) {

    public static EclassAssignmentListResponse unlinked() {
        return new EclassAssignmentListResponse(false, null, List.of());
    }

    /**
     * 연동이 끊긴 상태에서는 과제를 가져올 수 없다. 빈 목록을 "과제 없음"으로 오해하지 않도록 상태를 함께 준다.
     */
    public static EclassAssignmentListResponse expired() {
        return new EclassAssignmentListResponse(true, EclassLinkStatus.EXPIRED, List.of());
    }

    public static EclassAssignmentListResponse of(List<EclassAssignmentResponse> assignments) {
        return new EclassAssignmentListResponse(true, EclassLinkStatus.ACTIVE, assignments);
    }
}
