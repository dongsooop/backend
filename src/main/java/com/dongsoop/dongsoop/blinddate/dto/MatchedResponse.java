package com.dongsoop.dongsoop.blinddate.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매칭 성공 응답
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MatchedResponse {
    private Long partnerId;
    private String partnerDepartment;
    private String chatRoomId;
}
