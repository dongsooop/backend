package com.dongsoop.dongsoop.blinddate.dto;

import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;

public record BlindDateJoinResult(

        ParticipantInfo participantInfo,
        String sessionId,
        int currentCount,
        int maxCount
) {
}
