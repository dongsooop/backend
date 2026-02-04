package com.dongsoop.dongsoop.blinddate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대기큐 참여 요청
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JoinQueueRequest {
    private Long memberId;
    private String gender;
    private String department;
}
