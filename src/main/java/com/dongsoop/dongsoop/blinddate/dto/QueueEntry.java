package com.dongsoop.dongsoop.blinddate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 대기큐 항목
 */
@Getter
@AllArgsConstructor
public class QueueEntry {
    private String sessionId;
    private Long memberId;
    private String gender;
    private String department;
    private Long joinedAt;
}
