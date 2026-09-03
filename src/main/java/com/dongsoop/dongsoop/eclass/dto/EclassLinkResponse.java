package com.dongsoop.dongsoop.eclass.dto;

import com.dongsoop.dongsoop.eclass.entity.EclassLink;
import com.dongsoop.dongsoop.eclass.entity.EclassLinkStatus;
import java.time.LocalDateTime;

public record EclassLinkResponse(

        boolean linked,
        EclassLinkStatus status,
        String moodleFullname,
        LocalDateTime lastSyncedAt
) {

    public static EclassLinkResponse unlinked() {
        return new EclassLinkResponse(false, null, null, null);
    }

    public static EclassLinkResponse from(EclassLink link) {
        return new EclassLinkResponse(true, link.getStatus(), link.getMoodleFullname(), link.getLastSyncedAt());
    }
}
